package com.cms.dto;

import lombok.Data;

@Data
public class SystemMessageRequest {
    private Long meetingId;
    private String content;
}