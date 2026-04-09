package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("meeting_participation")
public class MeetingParticipation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long meetingId;
    private Long userId;
    private LocalDateTime joinTime;
    private LocalDateTime leaveTime;
    private String status;  // 参会状态：已加入、已退出
    private String notes;   // 备注信息
}