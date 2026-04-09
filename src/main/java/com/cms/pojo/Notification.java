package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("notifications")
public class Notification {
    @TableId(type = IdType.ASSIGN_ID)
    private Long notificationId;
    private String title;
    private String content;
    private Long targetId;  // 目标ID（如会议ID）
    private String type;    // 通知类型：会议通知、系统通知
    private Date createdAt;
    private Long userId;  // 添加用户ID字段
}