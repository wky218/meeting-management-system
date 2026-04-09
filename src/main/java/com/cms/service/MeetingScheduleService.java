package com.cms.service;

import com.cms.common.Result;

public interface MeetingScheduleService {
    Result<?> getTodayMeetings();
    Result<?> getUserTodayMeetings(Long userId);
    Result<?> getUserUpcomingMeetings(Long userId);

    Result<?> getUpcomingMeetings();

    Result<?> getUserMeetings(Long userId);
}