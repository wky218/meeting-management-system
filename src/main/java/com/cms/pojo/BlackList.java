package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("blacklist")
public class BlackList {
    @TableId(type = IdType.ASSIGN_ID)
    private Long blacklist_id;
    private Long meetingId;
    private Long userId;
    private int duration;
    private LocalDateTime createdAt;
}