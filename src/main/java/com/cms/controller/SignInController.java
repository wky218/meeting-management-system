package com.cms.controller;

import com.cms.common.Result;
import com.cms.service.SignInService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/signin")
@RequiredArgsConstructor
public class SignInController {

    private final SignInService signInService;

    @PostMapping("/submit")
    public Result<?> signIn(
            @RequestParam Long meetingId,
            @RequestParam Long userId) {
        return signInService.signIn(meetingId, userId);
    }
    @GetMapping("/status")
    public Result<?> getSignInStatus(
            @RequestParam Long meetingId,
            @RequestParam Long userId) {
        return signInService.getSignInStatus(meetingId, userId);
    }
    @GetMapping("/statistics/{meetingId}")
    public Result<?> getMeetingStatistics(@PathVariable Long meetingId) {
        return signInService.getMeetingStatistics(meetingId);
    }
}