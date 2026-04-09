package com.cms.service;

import com.cms.common.Result;
import com.cms.pojo.Notification;

public interface NotificationService {
    Result<?> sendNotification(Notification notification);
    Result<?> getNotifications(Long targetId);
    Result<?> getSystemNotifications();
}