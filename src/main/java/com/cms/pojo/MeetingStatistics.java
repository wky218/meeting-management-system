package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("statistics")
public class MeetingStatistics {
    @TableId(type = IdType.ASSIGN_ID)
    private Long statId;
    private Long meetingId;
    private Integer totalParticipants;  // 总参会人数
    private Integer signedIn;           // 已签到人数
    private Integer leaveCount;         // 请假人数
}