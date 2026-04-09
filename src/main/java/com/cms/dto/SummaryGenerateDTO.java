package com.cms.dto;

import lombok.Data;

@Data
public class SummaryGenerateDTO {
    private Long meetingId;
    private String messageType;  // CHAT, VOICE, BOTH
    private Long creatorId;
}