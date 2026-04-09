package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cms.enums.MeetingVisibility;
import com.cms.vo.ParticipantVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("meetings")
public class Meeting {
    @TableId(type = IdType.ASSIGN_ID)
    private Long meetingId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long organizerId;
    private String location;
    private String status;
    private LocalDateTime createdAt;
    @TableField("visibility")
    private MeetingVisibility visibility;
    @TableField("meeting_type")
    private String meetingType;
    // 会议管理员列表，非数据库字段
    @TableField(exist = false)
    private List<Long> adminIds;
    @TableField(exist = false)
    private List<Map<String, Object>> managers;
    private String joinPassword;
    @TableField(exist = false)
    private List<ParticipantVO> participants;
}