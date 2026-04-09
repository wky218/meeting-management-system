package com.cms.pojo;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cms.enums.ParticipantRole;
import lombok.Data;
import java.util.Date;

@Data
@TableName("meeting_participants")
public class MeetingParticipant {
    @TableId(type = IdType.ASSIGN_ID)
    private Long participantId;
    private Long meetingId;
    private Long userId;
    @TableField("role")
    private ParticipantRole role;  // 参会角色：发起者、管理员、与会者
    @TableField("sign_in_time")
    private Date signInTime;
    private String signInStatus;  // 签到状态：未签到、已签到
    private String leaveStatus;   // 请假状态：正常、请假

    @TableField("last_active_time")
    private Date lastActiveTime;  // 最后活跃时间
    @TableField("muted") // 如果数据库列名是 muted
    private boolean muted;
}