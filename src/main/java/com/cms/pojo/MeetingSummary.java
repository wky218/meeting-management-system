package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("meeting_summaries")
public class MeetingSummary {
    @TableId(type = IdType.ASSIGN_ID)
    private Long summaryId;
    private Long meetingId;
    private String content;
    private String status; // 状态：草稿、审核中、已确认
    private String messageType; // 消息类型：文字、语音、两者都有
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long creatorId;
    private Long reviewerId;//审核者id
}