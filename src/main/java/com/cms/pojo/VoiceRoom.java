package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("voice_rooms")
public class VoiceRoom {
    @TableId(type = IdType.ASSIGN_ID)
    private Long roomId;
    private Long meetingId;
    private Long creatorId;
    private String roomName;
    private String status;
    private Boolean recordEnabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}