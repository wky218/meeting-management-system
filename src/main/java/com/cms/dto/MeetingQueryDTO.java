package com.cms.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MeetingQueryDTO {
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}