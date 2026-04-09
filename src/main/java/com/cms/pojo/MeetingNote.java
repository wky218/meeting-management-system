package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("meeting_notes")
public class MeetingNote {
    @TableId(type = IdType.ASSIGN_ID)
    private Long noteId;

    private Long meetingId;

    private Long userId;

    private String noteName;

    private String noteContent;

    private Boolean isPublic;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}