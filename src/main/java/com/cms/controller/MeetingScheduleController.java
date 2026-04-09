package com.cms.controller;

import com.cms.common.Result;
import com.cms.service.MeetingScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class MeetingScheduleController {

    private final MeetingScheduleService scheduleService;

    @GetMapping("/today")
    public Result<?> getTodaySchedule() {
        return scheduleService.getTodayMeetings();
    }

    @GetMapping("/today/user/{userId}")
    public Result<?> getUserTodaySchedule(@PathVariable Long userId) {
        log.info("获取用户今日会议安排，用户ID：{}", userId);
        return scheduleService.getUserTodayMeetings(userId);
    }

    @GetMapping("/upcoming/user/{userId}")
    public Result<?> getUserUpcomingSchedule(@PathVariable Long userId) {
        log.info("获取用户即将开始的会议，用户ID：{}", userId);
        return scheduleService.getUserUpcomingMeetings(userId);
    }

    @GetMapping("/user/{userId}")
    public Result<?> getUserSchedule(@PathVariable Long userId) {
        log.info("获取用户所有会议，用户ID：{}", userId);
        return scheduleService.getUserMeetings(userId);
    }
}