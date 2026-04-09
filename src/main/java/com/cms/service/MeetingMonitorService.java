package com.cms.service;

import com.cms.common.Result;
import org.springframework.scheduling.annotation.Scheduled;

public interface MeetingMonitorService {
    Result<?> getOnlineParticipants(Long meetingId);
    Result<?> updateParticipantStatus(Long meetingId, Long userId);
    void checkInactiveParticipants();
}