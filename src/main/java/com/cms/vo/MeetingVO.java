package com.cms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class MeetingVO {
    private Long meetingId;
    private String title;
    private String description;
    private String location;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private String status;
    private Boolean isToday;           // 是否是今天的会议
    private Boolean isUpcoming;        // 是否即将开始（30分钟内）
    private String reminderMessage;    // 提醒消息
    private Long organizerId;        // 组织者ID
    private String meetingType;      // 会议类型
    private String organizerName;    // 组织者姓名
}