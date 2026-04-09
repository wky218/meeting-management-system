package com.cms.dto;

import lombok.Data;
import java.util.List;

@Data
public class VoiceRoomDTO {
    private Long meetingId;
    private Long creatorId;
    private String roomName;
    private List<Long> participants;
    private String status;  // ACTIVE, INACTIVE
    private Boolean recordEnabled;
}