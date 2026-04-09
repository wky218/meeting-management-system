package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebRTCMessage {
    private String type;        // 消息类型：join, offer, answer, candidate, leave
    private String roomId;      // 会议房间ID
    private String userId;      // 发送者ID
    private String targetUserId;// 接收者ID
    private Object payload;     // SDP 或 ICE candidate 数据
}