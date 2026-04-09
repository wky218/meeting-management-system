package com.cms.controller;

import com.cms.common.Result;
import com.cms.service.MeetingRecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings/recommend")
@RequiredArgsConstructor
public class MeetingRecommendController {

    private final MeetingRecommendService meetingRecommendService;

    @GetMapping("/public/{userId}")
    public Result<?> getRecommendedPublicMeetings(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "5") Integer limit) {
        return meetingRecommendService.recommendPublicMeetings(userId, limit);
    }

    @GetMapping("/private/{userId}")
    public Result<?> getUserPrivateMeetings(@PathVariable Long userId) {
        return meetingRecommendService.getPrivateMeetings(userId);
    }
}