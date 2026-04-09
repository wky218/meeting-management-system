package com.cms.dto;

import lombok.Data;

@Data
public class MeetingNoteDTO {
    private Long noteId;
    private Long meetingId;
    private Long userId;
    private String noteName;
    private String noteContent;
    private Boolean isPublic;
    private String createdAt;
    private String updatedAt;
}