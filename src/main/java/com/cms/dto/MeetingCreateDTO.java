package com.cms.dto;

import com.cms.enums.MeetingVisibility;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MeetingCreateDTO {
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private Long organizerId;
    private List<Long> participantIds;
    private MeetingVisibility visibility = MeetingVisibility.PUBLIC; // 默认公开
    private String meetingType;
    private List<Long> adminIds; // 会议管理员ID列表
    private String joinPassword;
}