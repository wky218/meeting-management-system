package com.cms.service.impl;

import com.cms.common.Result;
import com.cms.service.VoiceRoomManager;
import com.cms.service.WebRTCService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebRTCServiceImpl implements WebRTCService {
    // TURN/STUN 服务器配置
    private static final String TURN_SERVER = "turn:fwwhub.fun:3478";
    private static final String TURN_USERNAME = "user1";
    private static final String TURN_CREDENTIAL = "password1";
    private static final String STUN_SERVER = "stun:stun.l.google.com:19302";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VoiceRoomManager roomManager;

    // WebSocket会话管理
    private final Map<Long, Map<Long, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    public Result<?> handleJoinRoom(Long meetingId, Long userId, WebSocketSession session) {
        try {
            if (meetingId == null || userId == null) {
                log.error("加入房间失败：meetingId或userId为空 - meetingId: {}, userId: {}", meetingId, userId);
                return Result.error("meetingId和userId不能为空");
            }

            log.info("用户尝试加入房间 - userId: {}, meetingId: {}", userId, meetingId);

            // 使用VoiceRoomManager检查用户是否已在房间中
            if (roomManager.isParticipantInRoom(meetingId, userId)) {
                log.warn("用户已在房间中 - userId: {}, meetingId: {}", userId, meetingId);
                return Result.error("用户已在房间中");
            }

            // 添加用户到房间管理器
            roomManager.addParticipant(meetingId, userId);
            roomManager.updateAudioState(meetingId, userId, true);

            // 只有在session不为null时才保存WebSocket会话
            if (session != null) {
                roomSessions.computeIfAbsent(meetingId, k -> new ConcurrentHashMap<>())
                        .put(userId, session);

                // 通知房间内其他用户有新用户加入
                Map<String, Object> joinMessage = new HashMap<>();
                joinMessage.put("type", "user_joined");
                joinMessage.put("userId", userId);
                broadcastMessage(meetingId, userId, joinMessage);
            }

            return Result.success(roomManager.getRoomParticipants(meetingId));
        } catch (Exception e) {
            log.error("加入房间失败 - meetingId: {}, userId: {}, error: {}",
                    meetingId, userId, e.getMessage(), e);
            return Result.error("加入房间失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> handleLeaveRoom(Long meetingId, Long userId) {
        try {
            if (meetingId == null || userId == null) {
                log.error("离开房间失败：meetingId或userId为空 - meetingId: {}, userId: {}", meetingId, userId);
                return Result.error("meetingId和userId不能为空");
            }
            log.info("用户请求离开房间 - userId: {}, meetingId: {}", userId, meetingId);

            // 从VoiceRoomManager移除参与者
            boolean removed = roomManager.removeParticipant(meetingId, userId);
            if (!removed) {
                log.warn("用户不在房间中 - userId: {}, meetingId: {}", userId, meetingId);
                return Result.error("用户不在房间中");
            }

            // 移除WebSocket会话并通知其他用户
            Map<Long, WebSocketSession> room = roomSessions.get(meetingId);
            if (room != null) {
                room.remove(userId);
                if (room.isEmpty()) {
                    roomSessions.remove(meetingId);
                } else {
                    Map<String, Object> leaveMessage = new HashMap<>();
                    leaveMessage.put("type", "user_left");
                    leaveMessage.put("userId", userId);
                    broadcastMessage(meetingId, userId, leaveMessage);
                }
            }

            log.info("用户成功离开房间 - userId: {}, meetingId: {}", userId, meetingId);
            return Result.success("离开房间成功");
        } catch (Exception e) {
            log.error("离开房间失败 - meetingId: {}, userId: {}, error: {}",
                    meetingId, userId, e.getMessage(), e);
            return Result.error("离开房间失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getIceServers() {
        Map<String, Object> iceConfig = new HashMap<>();
        List<Map<String, Object>> iceServers = new ArrayList<>();

        Map<String, Object> stunServer = new HashMap<>();
        stunServer.put("urls", STUN_SERVER);
        iceServers.add(stunServer);

        Map<String, Object> turnServer = new HashMap<>();
        turnServer.put("urls", TURN_SERVER);
        turnServer.put("username", TURN_USERNAME);
        turnServer.put("credential", TURN_CREDENTIAL);
        iceServers.add(turnServer);

        iceConfig.put("iceServers", iceServers);
        return iceConfig;
    }

    @Override
    public void forwardSignal(Long meetingId, Long fromUserId, Long toUserId, Map<String, Object> signal) {
        try {
            Map<Long, WebSocketSession> room = roomSessions.get(meetingId);
            if (room != null) {
                WebSocketSession targetSession = room.get(toUserId);
                if (targetSession != null && targetSession.isOpen()) {
                    signal.put("from", fromUserId);
                    String message = objectMapper.writeValueAsString(signal);
                    targetSession.sendMessage(new TextMessage(message));
                    log.debug("信令已转发 - from: {}, to: {}, meetingId: {}", fromUserId, toUserId, meetingId);
                }
            }
        } catch (IOException e) {
            log.error("转发信令失败 - from: {}, to: {}, meetingId: {}, error: {}",
                    fromUserId, toUserId, meetingId, e.getMessage(), e);
        }
    }

    @Override
    public Result<?> handleAudioStreamChange(Long meetingId, Long userId, boolean isEnabled) {
        try {
            if (meetingId == null || userId == null) {
                log.error("音频状态更新失败：meetingId或userId为空 - meetingId: {}, userId: {}",
                        meetingId, userId);
                return Result.error("meetingId和userId不能为空");
            }

            if (!roomManager.isParticipantInRoom(meetingId, userId)) {
                log.error("用户不在房间中 - userId: {}, meetingId: {}", userId, meetingId);
                return Result.error("用户不在房间中");
            }

            // 更新音频状态
            roomManager.updateAudioState(meetingId, userId, isEnabled);
            log.info("用户音频状态已更新 - userId: {}, meetingId: {}, enabled: {}",
                    userId, meetingId, isEnabled);

            // 通知房间内其他用户音频状态变更
            Map<String, Object> audioMessage = new HashMap<>();
            audioMessage.put("type", "audio_state_change");
            audioMessage.put("userId", userId);
            audioMessage.put("isEnabled", isEnabled);
            broadcastMessage(meetingId, userId, audioMessage);

            return Result.success("音频状态更新成功");
        } catch (Exception e) {
            log.error("处理音频流状态变更失败 - meetingId: {}, userId: {}, error: {}",
                    meetingId, userId, e.getMessage(), e);
            return Result.error("处理音频流状态变更失败: " + e.getMessage());
        }
    }

    // 私有辅助方法，用于广播消息
    private void broadcastMessage(Long meetingId, Long excludeUserId, Map<String, Object> message) {
        try {
            Map<Long, WebSocketSession> room = roomSessions.get(meetingId);
            if (room != null) {
                String messageStr = objectMapper.writeValueAsString(message);
                room.forEach((userId, session) -> {
                    if (!userId.equals(excludeUserId) && session != null && session.isOpen()) {
                        try {
                            session.sendMessage(new TextMessage(messageStr));
                        } catch (IOException e) {
                            log.error("发送消息到用户失败 - userId: {}, meetingId: {}", userId, meetingId, e);
                        }
                    }
                });
            }
        } catch (IOException e) {
            log.error("广播消息序列化失败 - meetingId: {}", meetingId, e);
        }
    }
    @Override
    public void handleOffer(Long meetingId, Long fromUserId, Long toUserId, String sdp) {
        Map<String, Object> signal = new HashMap<>();
        signal.put("type", "offer");
        signal.put("sdp", sdp);
        forwardSignal(meetingId, fromUserId, toUserId, signal);
    }

    @Override
    public void handleAnswer(Long meetingId, Long fromUserId, Long toUserId, String sdp) {
        Map<String, Object> signal = new HashMap<>();
        signal.put("type", "answer");
        signal.put("sdp", sdp);
        forwardSignal(meetingId, fromUserId, toUserId, signal);
    }

    @Override
    public void handleIceCandidate(Long meetingId, Long fromUserId, Long toUserId, Map<String, Object> candidate) {
        Map<String, Object> signal = new HashMap<>();
        signal.put("type", "ice-candidate");
        signal.put("candidate", candidate);
        forwardSignal(meetingId, fromUserId, toUserId, signal);
    }
}