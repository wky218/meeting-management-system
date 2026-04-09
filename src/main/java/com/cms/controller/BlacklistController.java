package com.cms.controller;

import com.cms.pojo.BlackList;
import com.cms.service.BlacklistService;
import com.cms.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistService blacklistService;

    @Data
    public static class BlacklistRequest {
        private Long meetingId;
        private Long userId;
        private int duration;
        private Long operatorId;
    }

    @PostMapping("/add")
    public Result<?> addToBlacklist(@RequestBody BlacklistRequest request) {
        boolean success = blacklistService.addToBlacklist(
            request.getMeetingId(),
            request.getUserId(),
            request.getDuration(),
            request.getOperatorId()
        );
        return success ? Result.success("添加黑名单成功") : Result.error("添加黑名单失败");
    }

    @PostMapping("/remove")
    public Result<?> removeFromBlacklist(@RequestBody BlacklistRequest request) {
        boolean success = blacklistService.removeFromBlacklist(
            request.getMeetingId(),
            request.getUserId(),
            request.getOperatorId()
        );
        return success ? Result.success("移除黑名单成功") : Result.error("移除黑名单失败");
    }

    @GetMapping("/list/{meetingId}")
    public Result<List<BlackList>> getMeetingBlacklist(@PathVariable Long meetingId) {
        List<BlackList> blacklist = blacklistService.getMeetingBlacklist(meetingId);
        return Result.success(blacklist);
    }

    @GetMapping("/check")
    public Result<Boolean> checkUserInBlacklist(
            @RequestParam Long meetingId,
            @RequestParam Long userId) {
        boolean isBlacklisted = blacklistService.isUserInBlacklist(meetingId, userId);
        return Result.success(isBlacklisted);
    }
} 