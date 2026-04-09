package com.cms.controller;

import com.cms.common.Result;
import com.cms.dto.ParticipantDTO;
import com.cms.service.MeetingMonitorService;
import com.cms.service.UserConnectionService;
import com.cms.service.VoiceChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MeetingMonitorController {

    private final MeetingMonitorService monitorService;
    private final UserConnectionService userConnectionService;
    private final VoiceChatService voiceChatService;

    @PostMapping("/heartbeat")
    public Result<?> updateStatus(
            @RequestParam Long meetingId,
            @RequestParam Long userId) {
        return monitorService.updateParticipantStatus(meetingId, userId);
    }

    @GetMapping("/online/{meetingId}")
    public Result<?> getOnlineParticipants(@PathVariable Long meetingId) {
        return monitorService.getOnlineParticipants(meetingId);
    }
    @GetMapping("/getParticipantsList/{meetingId}")
    public Result<List<ParticipantDTO>> getConnectedUsers(@PathVariable Long meetingId) {
        return Result.success(voiceChatService.getParticipants(meetingId));
    }
}