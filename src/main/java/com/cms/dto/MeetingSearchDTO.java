package com.cms.dto;

import lombok.Data;

@Data
public class MeetingSearchDTO {
    private String meetingId;        // 会议号
    private String title;            // 会议名称
    private String meetingType;      // 会议类型
    private Long userId;  // 添加用户ID字段
}