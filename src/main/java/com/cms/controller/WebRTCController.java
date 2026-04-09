package com.cms.controller;

import com.cms.common.Result;
import com.cms.service.WebRTCService;
import com.cms.service.VoiceRoomManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webrtc")
@RequiredArgsConstructor
public class WebRTCController {

    private final WebRTCService webRTCService;
    private final VoiceRoomManager roomManager;

    @GetMapping("/ice-servers")
    public Result<?> getIceServers() {
        log.info("获取ICE服务器配置");
        return Result.success(webRTCService.getIceServers());
    }

    @PostMapping("/room/{meetingId}/user/{userId}/join")
    public Result<?> joinRoom(
            @PathVariable Long meetingId,
            @PathVariable Long userId) {
        try {
            log.info("用户请求加入房间 - userId: {}, meetingId: {}", userId, meetingId);
            return webRTCService.handleJoinRoom(meetingId, userId, null);
        } catch (Exception e) {
            log.error("加入房间异常 - userId: {}, meetingId: {}, error: {}", userId, meetingId, e.getMessage(), e);
            return Result.error("加入房间失败: " + e.getMessage());
        }
    }

    @GetMapping("/room/{meetingId}/participants")
    public Result<?> getRoomParticipants(@PathVariable Long meetingId) {
        log.info("获取房间 {} 的参与者列表", meetingId);
        return Result.success(roomManager.getRoomParticipants(meetingId));
    }

    @PostMapping("/room/{meetingId}/user/{userId}/leave")
    public Result<?> leaveRoom(
            @PathVariable Long meetingId,
            @PathVariable Long userId) {
        log.info("用户 {} 请求离开房间 {}", userId, meetingId);
        return webRTCService.handleLeaveRoom(meetingId, userId);
    }

    @PostMapping("/room/{meetingId}/user/{userId}/audio")
    public Result<?> handleAudioControl(
            @PathVariable Long meetingId,
            @PathVariable Long userId,
            @RequestParam boolean enabled) {
        log.info("用户 {} 在房间 {} 中{}音频", userId, meetingId, enabled ? "启用" : "禁用");
        return webRTCService.handleAudioStreamChange(meetingId, userId, enabled);
    }
}