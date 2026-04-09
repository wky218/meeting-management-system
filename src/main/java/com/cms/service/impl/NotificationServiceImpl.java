package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.common.Result;
import com.cms.mapper.MeetingParticipantMapper;
import com.cms.mapper.NotificationMapper;
import com.cms.pojo.Notification;
import com.cms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final MeetingParticipantMapper participantMapper;

    @Override
    public Result<?> sendNotification(Notification notification) {
        notification.setCreatedAt(new Date());
        if (notificationMapper.insert(notification) > 0) {
            return Result.success("通知发送成功");
        }
        return Result.error("通知发送失败");
    }

    @Override
    public Result<?> getNotifications(Long targetId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getTargetId, targetId)
                .eq(Notification::getType, "会议通知")
                .orderByDesc(Notification::getCreatedAt);

        List<Notification> notifications = notificationMapper.selectList(wrapper);
        return Result.success(notifications);
    }

    @Override
    public Result<?> getSystemNotifications() {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getType, "系统通知")
                .orderByDesc(Notification::getCreatedAt);

        List<Notification> notifications = notificationMapper.selectList(wrapper);
        return Result.success(notifications);
    }
}