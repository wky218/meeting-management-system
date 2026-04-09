package com.cms.dto;

import lombok.Data;

@Data
public class SummaryUpdateDTO {
    private Long summaryId;
    private String content;
    private Long reviewerId;
}