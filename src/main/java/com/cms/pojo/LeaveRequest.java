package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("leave_requests")
public class LeaveRequest {
    @TableId(type = IdType.ASSIGN_ID)
    private Long leaveId;
    private Long userId;
    private Long meetingId;
    private String reason;
    private String status;  // 待审批、通过、拒绝
    @TableField("created_at")
    private Date createdAt;
}