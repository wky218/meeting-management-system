package com.cms.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.mapper.MeetingMapper;
import com.cms.mapper.MeetingParticipantMapper;
import com.cms.pojo.Meeting;
import com.cms.pojo.Notification;
import com.cms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MeetingReminderTask {
    private final MeetingMapper meetingMapper;
    private final MeetingParticipantMapper participantMapper; // 添加参会者Mapper
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * ?")
    public void dailyMeetingReminder() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        LambdaQueryWrapper<Meeting> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(Meeting::getStartTime,
                        Date.from(todayStart.atZone(ZoneId.systemDefault()).toInstant()),
                        Date.from(todayEnd.atZone(ZoneId.systemDefault()).toInstant()))
                .eq(Meeting::getStatus, "未开始"); // 只提醒未开始的会议

        List<Meeting> todayMeetings = meetingMapper.selectList(wrapper);

        for (Meeting meeting : todayMeetings) {
            // 获取会议参与者
            List<Long> participantIds = participantMapper.selectParticipantIdsByMeetingId(meeting.getMeetingId());

            for (Long participantId : participantIds) {
                Notification notification = new Notification();
                notification.setTitle("今日会议提醒");
                notification.setContent(String.format("您今日有会议：%s，地点：%s，时间：%s",
                        meeting.getTitle(),
                        meeting.getLocation(),
                        meeting.getStartTime()));
                notification.setTargetId(meeting.getMeetingId());
                notification.setType("会议通知");
                notification.setUserId(participantId); // 添加接收者ID

                notificationService.sendNotification(notification);
            }
        }
    }

    @Scheduled(cron = "0 * * * * ?")
    public void meetingStartReminder() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyMinutesLater = now.plusMinutes(30);

        LambdaQueryWrapper<Meeting> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(Meeting::getStartTime,
                        Date.from(now.atZone(ZoneId.systemDefault()).toInstant()),
                        Date.from(thirtyMinutesLater.atZone(ZoneId.systemDefault()).toInstant()))
                .eq(Meeting::getStatus, "未开始"); // 只提醒未开始的会议

        List<Meeting> upcomingMeetings = meetingMapper.selectList(wrapper);

        for (Meeting meeting : upcomingMeetings) {
            // 获取会议参与者
            List<Long> participantIds = participantMapper.selectParticipantIdsByMeetingId(meeting.getMeetingId());

            for (Long participantId : participantIds) {
                Notification notification = new Notification();
                notification.setTitle("会议即将开始");
                notification.setContent(String.format("您的会议《%s》将在30分钟后开始，地点：%s，请做好准备",
                        meeting.getTitle(),
                        meeting.getLocation()));
                notification.setTargetId(meeting.getMeetingId());
                notification.setType("会议通知");
                notification.setUserId(participantId); // 添加接收者ID

                notificationService.sendNotification(notification);
            }
        }
    }
}