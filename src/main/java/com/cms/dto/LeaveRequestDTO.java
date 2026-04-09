package com.cms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

@Data
public class LeaveRequestDTO {
    private Long userId;
    private Long meetingId;
    private String reason;
}