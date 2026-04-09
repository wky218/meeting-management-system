package com.cms.controller;

import com.cms.common.Result;
import com.cms.dto.VoiceRoomDTO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.cms.service.VoiceChatService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceChatController {

    private final VoiceChatService voiceChatService;

    @GetMapping("/status/{meetingId}")
    public Result<?> getVoiceChatStatus(@PathVariable Long meetingId) {
        try {
            log.info("获取会议语音状态, meetingId: {}", meetingId);
            return voiceChatService.getVoiceChatStatus(meetingId);
        } catch (Exception e) {
            log.error("获取语音状态失败: {}", e.getMessage(), e);
            return Result.error("获取语音状态失败");
        }
    }

    @GetMapping("/join/{meetingId}/{userId}")
    public Result<?> joinMeeting(@PathVariable Long meetingId, @PathVariable Long userId) {
        try {
            log.info("用户请求加入语音会议, meetingId: {}, userId: {}", meetingId, userId);

            // 检查参数
            if (meetingId == null || userId == null) {
                return Result.error("参数不能为空");
            }

            if (!voiceChatService.checkUserPermission(meetingId, userId)) {
                log.warn("用户权限验证失败, meetingId: {}, userId: {}", meetingId, userId);
                return Result.error("该用户不在会议成员列表中");
            }

            // 获取服务器配置
            String serverHost = "localhost:8080";  // TODO: 从配置文件获取
            String wsProtocol = "ws";

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("wsUrl", String.format("%s://%s/voice?meetingId=%d&userId=%d",
                    wsProtocol, serverHost, meetingId, userId));
            data.put("iceServers", getIceServers());

            log.info("用户成功加入语音会议, meetingId: {}, userId: {}", meetingId, userId);
            return Result.success(data);
        } catch (Exception e) {
            log.error("加入语音会议失败: {}", e.getMessage(), e);
            return Result.error("加入语音会议失败: " + e.getMessage());
        }
    }

//    @PostMapping("/mute/{meetingId}/{userId}")
//    public Result<?> toggleMute(@PathVariable Long meetingId, @PathVariable Long userId) {
//        try {
//            log.info("切换用户静音状态, meetingId: {}, userId: {}", meetingId, userId);
//            return voiceChatService.toggleMute(meetingId, userId);
//        } catch (Exception e) {
//            log.error("切换静音状态失败: {}", e.getMessage(), e);
//            return Result.error("切换静音状态失败");
//        }
//    }

    @PostMapping("/mute/{meetingId}/{userId}")
    public Result<?> setMute(
            @PathVariable Long meetingId,
            @PathVariable Long userId,
            @RequestBody MuteRequest request
    ) {
        try {
            return voiceChatService.setMute(meetingId, userId, request.isMuted());
        } catch (Exception e) {
            log.error("设置静音状态失败: {}", e.getMessage(), e);
            return Result.error("设置静音状态失败");
        }
    }

    @Data
    public static class MuteRequest {
        private boolean muted;
    }

    private List<Map<String, String>> getIceServers() {
        List<Map<String, String>> iceServers = new ArrayList<>();

        // 添加 STUN 服务器
        Map<String, String> stunServer = new HashMap<>();
        stunServer.put("urls", "stun:stun.l.google.com:19302");
        iceServers.add(stunServer);
        // TODO: 添加 TURN 服务器配置
        return iceServers;
    }
    @PostMapping("/room")
    public Result<?> createVoiceRoom(@RequestBody VoiceRoomDTO roomDTO) {
        try {
            log.info("创建语音房间, meetingId: {}", roomDTO.getMeetingId());
            return voiceChatService.createVoiceRoom(roomDTO);
        } catch (Exception e) {
            log.error("创建语音房间失败: {}", e.getMessage(), e);
            return Result.error("创建语音房间失败");
        }
    }

    @GetMapping("/room/{meetingId}")
    public Result<?> getVoiceRoomInfo(@PathVariable Long meetingId) {
        try {
            log.info("获取语音房间信息, meetingId: {}", meetingId);
            return voiceChatService.getVoiceRoomInfo(meetingId);
        } catch (Exception e) {
            log.error("获取语音房间信息失败: {}", e.getMessage(), e);
            return Result.error("获取语音房间信息失败");
        }
    }

    @PutMapping("/room/{meetingId}/status")
    public Result<?> updateVoiceRoomStatus(
            @PathVariable Long meetingId,
            @RequestParam String status) {
        try {
            log.info("更新语音房间状态, meetingId: {}, status: {}", meetingId, status);
            return voiceChatService.updateVoiceRoomStatus(meetingId, status);
        } catch (Exception e) {
            log.error("更新语音房间状态失败: {}", e.getMessage(), e);
            return Result.error("更新语音房间状态失败");
        }
    }
}