package com.cms.pojo;

import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
public class VoiceSession {
    private String sessionId;
    private Long userId;
    private Long meetingId;
    private WebSocketSession webSocketSession;
    private boolean muted;
    private String peerConnectionId;  // 添加 WebRTC 相关字段
    private String sdpOffer;         // 添加 SDP offer
    private String iceCandidate;     // 添加 ICE candidate
}