package com.cms.service;

import com.cms.common.Result;
import com.cms.dto.ParticipantDTO;
import com.cms.dto.VoiceRoomDTO;

import java.util.List;

public interface VoiceChatService {
    Result<?> getVoiceChatStatus(Long meetingId);
    boolean checkUserPermission(Long meetingId, Long userId);
    Result<?> toggleMute(Long meetingId, Long userId);
    Result<?> createVoiceRoom(VoiceRoomDTO roomDTO);
    Result<?> getVoiceRoomInfo(Long meetingId);
    Result<?> updateVoiceRoomStatus(Long meetingId, String status);
    Result<?> handleUserLeave(Long meetingId, Long userId);

    Result<?> setMute(Long meetingId, Long userId, boolean muted);

    List<ParticipantDTO> getParticipants(Long meetingId);
}