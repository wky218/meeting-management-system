package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
@Data
@TableName("chat_messages")
public class ChatMessage {
    @TableId(type = IdType.ASSIGN_ID)
    private Long messageId;
    private Long meetingId;
    private Long userId;
    private String senderName;
    private String content;
    private String messageType;
    private LocalDateTime sendTime;
    private LocalDateTime updateTime;
    // 保留无参构造方法
    public ChatMessage() {
    }
}