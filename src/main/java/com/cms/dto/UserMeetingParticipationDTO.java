package com.cms.dto;

import com.cms.enums.MeetingVisibility;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserMeetingParticipationDTO {
    private Long meetingId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private Long organizerId;
    private List<Long> participantIds;
    private MeetingVisibility visibility;
    private String meetingType;
    private List<Long> adminIds;
    private String joinPassword;
    private String signInStatus;
    // 参会记录相关信息
    private LocalDateTime joinTime;
    private LocalDateTime leaveTime;
    private Long duration;  // 参会时长
    private String durationDisplay; // 格式化的时长显示（例如：2时30分）

}