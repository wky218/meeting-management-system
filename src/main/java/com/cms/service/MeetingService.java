package com.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.common.Result;
import com.cms.dto.MeetingCreateDTO;
import com.cms.dto.MeetingQueryDTO;
import com.cms.dto.MeetingSearchDTO;
import com.cms.dto.UserMeetingParticipationDTO;
import com.cms.enums.MeetingVisibility;
import com.cms.pojo.Meeting;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface MeetingService {
    @Transactional
    Result<?> createPublicMeeting(MeetingCreateDTO meetingDTO);

    Result<?> createMeeting(MeetingCreateDTO meetingDTO);
    Result<Page<Meeting>> listMeetings(MeetingQueryDTO queryDTO);
    Result<Meeting> getMeetingDetail(Long id);
    boolean isParticipant(Long meetingId, Long userId); // 判断用户是否为会议参与者
    Result<?> cancelMeeting(Long meetingId,Long userId);

    Result<?> getUserMeetings(Long userId);
    Result<?> startMeeting(Long meetingId);
    Result<?> getMeetingParticipants(Long meetingId);
    Result<?> searchPublicMeetings(MeetingSearchDTO searchDTO);

    Result<?> searchPrivateMeetings(MeetingSearchDTO searchDTO);

    @Transactional
    Result<?> joinMeeting(Long meetingId, Long userId, String password);

    @Transactional
    Result<?> updateMeeting(Long meetingId, String title, String description,
                            LocalDateTime startTime, LocalDateTime endTime, String location,
                            MeetingVisibility visibility, String meetingType);

    Result<?> addMeetingAdmin(Long meetingId, Long userId, Long currentUserId);

    @Transactional
    Result<?> endMeeting(Long meetingId, Long userId);

    Result<?> removeMeetingAdmin(Long meetingId, Long userId, Long currentUserId);

    Result<?> getMeetingManagers(Long meetingId);

    List<UserMeetingParticipationDTO> getUserParticipationHistory(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    /**
     * 获取会议室中的所有用户ID
     * @param meetingId 会议ID
     * @return 用户ID列表
     */

}