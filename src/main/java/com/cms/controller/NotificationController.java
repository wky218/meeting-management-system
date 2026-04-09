package com.cms.controller;

import com.cms.common.Result;
import com.cms.pojo.Notification;
import com.cms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public Result<?> sendNotification(@RequestBody Notification notification) {
        return notificationService.sendNotification(notification);
    }

    @GetMapping("/meeting/{meetingId}")
    public Result<?> getMeetingNotifications(@PathVariable Long meetingId) {
        return notificationService.getNotifications(meetingId);
    }

    @GetMapping("/system")
    public Result<?> getSystemNotifications() {
        return notificationService.getSystemNotifications();
    }
}