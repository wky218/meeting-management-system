package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cms.common.Result;
import com.cms.dto.ParticipantDTO;
import com.cms.dto.VoiceRoomDTO;
import com.cms.enums.ParticipantRole;
import com.cms.mapper.MeetingParticipantMapper;
import com.cms.mapper.UserMapper;
import com.cms.pojo.MeetingParticipant;
import com.cms.pojo.User;
import com.cms.pojo.VoiceRoom;
import com.cms.mapper.VoiceRoomMapper;
import com.cms.service.MeetingService;
import com.cms.service.UserService;
import com.cms.service.VoiceChatService;
import com.cms.service.VoiceRoomManager;
import com.cms.websocket.ChatWebSocketHandler;
import com.cms.websocket.VoiceChatHandler;
import com.cms.websocket.WebSocketMessageQueueManager;
import com.cms.websocket.WebSocketSessionManager;
import com.cms.websocket.message.MuteMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceChatServiceImpl implements VoiceChatService {

    private final VoiceRoomMapper voiceRoomMapper;
    private final VoiceChatHandler voiceChatHandler;
    private final VoiceRoomManager roomManager;
    private final MeetingParticipantMapper meetingParticipantMapper;
    private final UserMapper userMapper;
    @Autowired
    private WebSocketSessionManager sessionManager;
    @Autowired
    private UserService userService;
    @Autowired
    private MeetingService meetingService;

    @Override
    @Transactional
    public Result<?> createVoiceRoom(VoiceRoomDTO roomDTO) {
        try {
            // 1. 创建数据库记录
            VoiceRoom room = new VoiceRoom();
            room.setMeetingId(roomDTO.getMeetingId());
            room.setCreatorId(roomDTO.getCreatorId());
            room.setRoomName(roomDTO.getRoomName());
            room.setStatus("ACTIVE");
            room.setRecordEnabled(roomDTO.getRecordEnabled());
            room.setCreateTime(LocalDateTime.now());
            room.setUpdateTime(LocalDateTime.now());

            voiceRoomMapper.insert(room);

            // 2. 初始化房间管理器中的房间
            return roomManager.createRoom(roomDTO);
        } catch (Exception e) {
            log.error("创建语音房间失败: {}", e.getMessage(), e);
            return Result.error("创建语音房间失败: " + e.getMessage());
        }
    }

    @Override
    public Result<?> getVoiceChatStatus(Long meetingId) {
        try {
            // 从房间管理器获取参与者信息
            Set<Long> participants = roomManager.getRoomParticipants(meetingId);

            Map<String, Object> status = new HashMap<>();
            status.put("participantCount", participants.size());
            status.put("participants", participants);
            status.put("active", !participants.isEmpty());
            status.put("recordEnabled", false); // 从数据库获取

            return Result.success(status);
        } catch (Exception e) {
            log.error("获取语音状态失败: {}", e.getMessage(), e);
            return Result.error("获取语音状态失败");
        }
    }

    @Override
    public boolean checkUserPermission(Long meetingId, Long userId) {
        // TODO: 实现实际的权限检查逻辑
        return true;
    }

    @Override
    public Result<?> toggleMute(Long meetingId, Long userId) {
        try {
            // 获取当前音频状态
            Result<?> roomInfoResult = roomManager.getRoomInfo(meetingId);
            if (roomInfoResult.getCode() != 200) {
                return Result.error("获取房间信息失败");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> roomInfo = (Map<String, Object>) roomInfoResult.getData();

            // 从roomInfo中获取audioStates
            @SuppressWarnings("unchecked")
            Map<Long, Boolean> audioStates = (Map<Long, Boolean>) roomInfo.get("audioStates");
            if (audioStates == null) {
                audioStates = new HashMap<>();
            }

            // 获取当前状态，默认为未静音
            boolean currentStatus = audioStates.getOrDefault(userId, true);

            // 更新音频状态
            roomManager.updateAudioState(meetingId, userId, !currentStatus);

            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("muted", currentStatus);
            return Result.success(result);
        } catch (Exception e) {
            log.error("切换静音状态失败: {}", e.getMessage(), e);
            return Result.error("切换静音状态失败");
        }
    }
    @Override
    public Result<?> getVoiceRoomInfo(Long meetingId) {
        try {
            // 1. 获取数据库中的房间信息
            VoiceRoom room = voiceRoomMapper.selectOne(
                    new QueryWrapper<VoiceRoom>().eq("meeting_id", meetingId)
            );

            if (room == null) {
                return Result.error("语音房间不存在");
            }

            // 2. 获取房间管理器中的实时信息
            Result<?> roomInfo = roomManager.getRoomInfo(meetingId);
            if (!roomInfo.getCode().equals(200)) {
                return roomInfo;
            }

            // 3. 合并信息
            Map<String, Object> info = new HashMap<>();
            info.put("room", room);
            info.put("realTimeInfo", roomInfo.getData());

            return Result.success(info);
        } catch (Exception e) {
            log.error("获取语音房间信息失败: {}", e.getMessage(), e);
            return Result.error("获取语音房间信息失败");
        }
    }

    @Override
    @Transactional
    public Result<?> updateVoiceRoomStatus(Long meetingId, String status) {
        try {
            // 1. 更新数据库状态
            VoiceRoom room = voiceRoomMapper.selectOne(
                    new QueryWrapper<VoiceRoom>().eq("meeting_id", meetingId)
            );

            if (room == null) {
                return Result.error("语音房间不存在");
            }

            room.setStatus(status);
            room.setUpdateTime(LocalDateTime.now());
            voiceRoomMapper.updateById(room);

            // 2. 如果房间状态为非活跃，清理房间管理器中的数据
            if ("INACTIVE".equals(status)) {
                roomManager.removeParticipant(meetingId, null); // 传null表示清理整个房间
            }

            return Result.success(room);
        } catch (Exception e) {
            log.error("更新房间状态失败: {}", e.getMessage(), e);
            return Result.error("更新房间状态失败");
        }
    }

    @Override
    public Result<?> handleUserLeave(Long meetingId, Long userId) {
        try {
            // 1. 从房间管理器移除用户
            boolean removed = roomManager.removeParticipant(meetingId, userId);
            if (!removed) {
                return Result.error("用户不在语音房间中");
            }

            // 2. 如果房间为空，更新数据库状态
            if (roomManager.getRoomParticipants(meetingId).isEmpty()) {
                VoiceRoom room = voiceRoomMapper.selectById(meetingId);
                if (room != null) {
                    room.setStatus("INACTIVE");
                    room.setUpdateTime(LocalDateTime.now());
                    voiceRoomMapper.updateById(room);
                }
            }

            return Result.success("用户已离开语音房间");
        } catch (Exception e) {
            log.error("处理用户离开语音房间时发生错误: meetingId={}, userId={}, error={}",
                    meetingId, userId, e.getMessage(), e);
            return Result.error("处理用户离开失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> setMute(Long meetingId, Long userId, boolean muted) {
        log.info("设置用户静音状态, meetingId: {}, userId: {}, muted: {}",
                meetingId, userId, muted);

        try {
            // 检查会议是否存在
            if (!checkMeetingExists(meetingId)) {
                return Result.error("会议不存在");
            }
            // <<<<<<<<<< 修改这里，使用 WebSocketMessageQueueManager 检查用户是否在线 >>>>>>>>>>
            if (!WebSocketMessageQueueManager.isUserInMeeting(meetingId, userId)) {
                return Result.error("用户不在会议中");
            }
            // 通过 WebSocketMessageQueueManager 发送消息
            MuteMessage muteMessage = new MuteMessage(userId, muted);

            String muteMessageContent = "{\"type\":\"MUTE\", \"userId\":" + userId + ", \"muted\":" + muted + "}"; // 示例 JSON 结构

            // 使用 WebSocketMessageQueueManager 广播消息
            WebSocketMessageQueueManager.broadcastMessageToSendQueue(meetingId.toString(), muteMessageContent);


            // 更新用户的静音状态
            updateUserMuteStatus(meetingId, userId, muted);

            return Result.success(muted);
        } catch (Exception e) {
            log.error("设置静音状态失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    private boolean checkMeetingExists(Long meetingId) {
        // TODO: 实现会议存在性检查
        return true;
    }

    private void updateUserMuteStatus(Long meetingId, Long userId, boolean muted) {
        log.debug("更新用户静音状态到数据库 - meetingId: {}, userId: {}, muted: {}", meetingId, userId, muted);
        try {
            LambdaQueryWrapper<MeetingParticipant> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                    .eq(MeetingParticipant::getUserId, userId);

            MeetingParticipant participant = meetingParticipantMapper.selectOne(queryWrapper);

            if (participant != null) {
                participant.setMuted(muted); // <<<<<<<<<< 设置静音状态字段
                meetingParticipantMapper.updateById(participant); // <<<<<<<<<< 更新到数据库
                log.info("用户静音状态更新成功 - meetingId: {}, userId: {}, muted: {}", meetingId, userId, muted);
            } else {
                log.warn("未找到对应的参会记录，无法更新静音状态 - meetingId: {}, userId: {}", meetingId, userId);
            }
        } catch (Exception e) {
            log.error("更新用户静音状态失败 - meetingId: {}, userId: {}", meetingId, userId, e);
        }
    }
    @Override
    public List<ParticipantDTO> getParticipants(Long meetingId) {
        // 1. 获取在线用户ID列表 (通过 WebSocket 会话管理器获取)
        List<String> connectedUserIdsStr = ChatWebSocketHandler.getConnectedUserIds(meetingId);

        // 如果没有在线用户，直接返回空列表
        if (connectedUserIdsStr.isEmpty()) {
            return Collections.emptyList();
        }

        // 将在线用户ID从 String 转换为 Long
        List<Long> connectedUserIds = connectedUserIdsStr.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

        // 2. 根据在线用户ID列表，查询数据库获取这些用户的参会信息 (角色、静音状态等)
        LambdaQueryWrapper<MeetingParticipant> participantQueryWrapper = new LambdaQueryWrapper<>();
        participantQueryWrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .in(MeetingParticipant::getUserId, connectedUserIds); // 只查询在线用户的参会记录

        List<MeetingParticipant> onlineParticipants = meetingParticipantMapper.selectList(participantQueryWrapper);

        // 3. 根据在线用户ID列表，查询数据库获取这些用户的基本信息 (用户名)
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getUserId, connectedUserIds); // 假设 User 实体有 userId 字段

        List<User> onlineUsers = userMapper.selectList(userQueryWrapper);

        // 将查询结果转换为 Map，方便通过 userId 查找
        Map<Long, MeetingParticipant> participantMap = onlineParticipants.stream()
                .collect(Collectors.toMap(MeetingParticipant::getUserId, p -> p));
        Map<Long, User> userMap = onlineUsers.stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));


        // 4. 构建 ParticipantDTO 列表，合并从数据库获取的信息
        return connectedUserIds.stream()
                .map(userId -> {
                    MeetingParticipant participant = participantMap.get(userId);
                    User user = userMap.get(userId);

                    // 理论上这里不应该为 null，因为 connectedUserIds 来自在线会话，
                    // 但为了健壮性，可以加上判断
                    if (participant == null || user == null) {
                        return null;
                    }

                    // 从 MeetingParticipant 实体获取静音状态
                    boolean isMuted = participant.isMuted(); // 或者 participant.getMuted();

                    return ParticipantDTO.builder()
                            .userId(userId)
                            .username(user.getUsername())
                            .role(participant.getRole())
                            .muted(isMuted) // 使用从数据库获取的静音状态
                            .build();
                })
                .filter(Objects::nonNull) // 过滤掉可能为 null 的情况
                .collect(Collectors.toList());
    }
    private boolean getMuteStatus(Long meetingId, Long userId) {
        log.debug("获取用户麦克风状态 - meetingId: {}, userId: {}", meetingId, userId);
        try {
            LambdaQueryWrapper<MeetingParticipant> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                    .eq(MeetingParticipant::getUserId, userId);

            MeetingParticipant participant = meetingParticipantMapper.selectOne(queryWrapper);

            if (participant != null) {
                // <<<<<<<<<< 从数据库读取静音状态字段 >>>>>>>>>>
                return participant.isMuted(); // 或者 participant.getMuted(); 如果字段类型是 Boolean
            } else {
                log.warn("未找到对应的参会记录，无法获取麦克风状态 - meetingId: {}, userId: {}", meetingId, userId);
                return false; // 如果找不到参会记录，默认认为未静音
            }
        } catch (Exception e) {
            log.error("获取用户麦克风状态失败 - meetingId: {}, userId={}", meetingId, userId, e);
            return true;  // 出错时默认为静音状态
        }
    }
}