package com.cms.service.impl;

import com.cms.common.Result;
import com.cms.mapper.MeetingMapper;
import com.cms.service.MeetingScheduleService;
import com.cms.vo.MeetingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingScheduleServiceImpl implements MeetingScheduleService {

    private final MeetingMapper meetingMapper;

    @Override
    public Result<?> getTodayMeetings() {
        List<MeetingVO> meetings = meetingMapper.getTodayMeetings();
        return Result.success(processMeetings(meetings));
    }

    @Override
    public Result<?> getUpcomingMeetings() {
        List<MeetingVO> meetings = meetingMapper.getUpcomingMeetings();
        return Result.success(processMeetings(meetings));
    }

    @Override
    public Result<?> getUserMeetings(Long userId) {
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }
        List<MeetingVO> meetings = meetingMapper.getUserMeetings(userId);
        return Result.success(processMeetings(meetings));
    }

    private List<MeetingVO> processMeetings(List<MeetingVO> meetings) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        for (MeetingVO meeting : meetings) {
            LocalDateTime meetingTime = meeting.getStartTime();

            if (meetingTime.toLocalDate().equals(today)) {
                meeting.setIsToday(true);
                if (meetingTime.isBefore(now)) {
                    meeting.setStatus("进行中");
                } else {
                    Duration duration = Duration.between(now, meetingTime);
                    if (duration.toMinutes() <= 30 && duration.toMinutes() > 0) {
                        meeting.setIsUpcoming(true);
                        meeting.setStatus("即将开始");
                        meeting.setReminderMessage(String.format("会议《%s》将在%d分钟后开始，请做好准备",
                                meeting.getTitle(), duration.toMinutes()));
                    } else {
                        meeting.setStatus("未开始");
                        meeting.setReminderMessage(String.format("您今天%s有会议：%s",
                                meetingTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                meeting.getTitle()));
                    }
                }
            } else if (meetingTime.toLocalDate().isAfter(today)) {
                meeting.setStatus("未开始");
            } else {
                meeting.setStatus("已结束");
            }
        }
        return meetings;
    }
    @Override
    public Result<?> getUserTodayMeetings(Long userId) {
        log.info("获取用户今日会议，用户ID：{}", userId);

        if (userId == null) {
            return Result.error("用户ID不能为空");
        }

        List<MeetingVO> meetings = meetingMapper.getUserTodayMeetings(userId);

        if (meetings == null || meetings.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        LocalDateTime now = LocalDateTime.now();

        for (MeetingVO meeting : meetings) {
            LocalDateTime meetingTime = meeting.getStartTime();

            meeting.setIsToday(true);
            if (meetingTime.isBefore(now)) {
                meeting.setStatus("进行中");
            } else {
                Duration duration = Duration.between(now, meetingTime);
                if (duration.toMinutes() <= 30) {
                    meeting.setIsUpcoming(true);
                    meeting.setStatus("即将开始");
                    meeting.setReminderMessage(String.format("会议《%s》将在%d分钟后开始，请做好准备",
                            meeting.getTitle(), duration.toMinutes()));
                } else {
                    meeting.setStatus("未开始");
                    meeting.setReminderMessage(String.format("您今天%s有会议：%s",
                            meetingTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            meeting.getTitle()));
                }
            }
        }

        return Result.success(meetings);
    }

    @Override
    public Result<?> getUserUpcomingMeetings(Long userId) {
        log.info("获取用户即将开始的会议，用户ID：{}", userId);

        if (userId == null) {
            return Result.error("用户ID不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyMinutesLater = now.plusMinutes(30);

        List<MeetingVO> meetings = meetingMapper.getUserUpcomingMeetings(userId, now, thirtyMinutesLater);

        if (meetings == null || meetings.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        for (MeetingVO meeting : meetings) {
            meeting.setIsUpcoming(true);
            meeting.setStatus("即将开始");
            Duration duration = Duration.between(now, meeting.getStartTime());
            meeting.setReminderMessage(String.format("会议《%s》将在%d分钟后开始，请做好准备",
                    meeting.getTitle(), duration.toMinutes()));
        }

        return Result.success(meetings);
    }
}