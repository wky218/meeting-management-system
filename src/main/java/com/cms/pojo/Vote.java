package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

@Data
@TableName("votes")
public class Vote {
    @TableId(type = IdType.ASSIGN_ID)
    private Long voteId;
    private Long meetingId;
    private String title;
    private String description;
    private Boolean isMultiple;    // 是否多选
    private Boolean isAnonymous;   // 是否匿名
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;     // 开始时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;           // 截止时间
    private String status;         // 状态：进行中、已结束
    @TableField("created_at")
    private Date createdAt;
}