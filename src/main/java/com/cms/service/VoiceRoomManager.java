package com.cms.service;

import com.cms.common.Result;
import com.cms.dto.VoiceRoomDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class VoiceRoomManager {
    // 存储房间信息
    private final Map<Long, Map<String, Object>> roomInfo = new ConcurrentHashMap<>();
    // 存储房间参与者
    private final Map<Long, Set<Long>> roomParticipants = new ConcurrentHashMap<>();
    // 存储音频状态
    private final Map<Long, Map<Long, Boolean>> audioStates = new ConcurrentHashMap<>();

//创建房间
    public Result<?> createRoom(VoiceRoomDTO dto) {
        Map<String, Object> info = new HashMap<>();
        info.put("meetingId", dto.getMeetingId());
        info.put("creatorId", dto.getCreatorId());
        info.put("createTime", System.currentTimeMillis());
        info.put("status", "ACTIVE");

        roomInfo.put(dto.getMeetingId(), info);
        roomParticipants.put(dto.getMeetingId(), ConcurrentHashMap.newKeySet());
        audioStates.put(dto.getMeetingId(), new ConcurrentHashMap<>());

        return Result.success(info);
    }

    //获取房间信息
    public Result<?> getRoomInfo(Long meetingId) {
        Map<String, Object> info = new HashMap<>();
        info.put("audioStates", audioStates.getOrDefault(meetingId, new ConcurrentHashMap<>()));
        info.put("participants", roomParticipants.getOrDefault(meetingId, ConcurrentHashMap.newKeySet()));
        info.put("status", "ACTIVE");
        return Result.success(info);
    }

//获取房间参与者列表
    public Set<Long> getRoomParticipants(Long meetingId) {
        return roomParticipants.getOrDefault(meetingId, ConcurrentHashMap.newKeySet());
    }

//添加参与者
    public boolean addParticipant(Long meetingId, Long userId) {
        Set<Long> participants = roomParticipants.computeIfAbsent(meetingId,
                k -> ConcurrentHashMap.newKeySet());

        // 添加参与者
        boolean added = participants.add(userId);

        // 设置初始音频状态
        if (added) {
            audioStates.computeIfAbsent(meetingId, k -> new ConcurrentHashMap<>())
                    .put(userId, true);
        }

        return added;
    }

//移除参与者
    public boolean removeParticipant(Long meetingId, Long userId) {
        Set<Long> participants = roomParticipants.get(meetingId);
        if (participants != null) {
            boolean removed = participants.remove(userId);

            // 移除音频状态
            Map<Long, Boolean> audioState = audioStates.get(meetingId);
            if (audioState != null) {
                audioState.remove(userId);
            }

            // 如果房间为空，清理房间
            if (participants.isEmpty()) {
                cleanupRoom(meetingId);
            }

            return removed;
        }
        return false;
    }

// 更新音频状态
    public void updateAudioState(Long meetingId, Long userId, boolean enabled) {
        audioStates.computeIfAbsent(meetingId, k -> new ConcurrentHashMap<>())
                .put(userId, enabled);
    }

//清理房间
    private void cleanupRoom(Long meetingId) {
        roomInfo.remove(meetingId);
        roomParticipants.remove(meetingId);
        audioStates.remove(meetingId);
    }
    public boolean isParticipantInRoom(Long meetingId, Long userId) {
        Set<Long> participants = roomParticipants.get(meetingId);
        return participants != null && participants.contains(userId);
    }

}