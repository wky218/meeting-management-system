package com.cms.service;

import com.cms.common.Result;
import org.springframework.web.socket.WebSocketSession;
import java.util.Map;

public interface WebRTCService {
    // 处理加入房间
    Result<?> handleJoinRoom(Long meetingId, Long userId, WebSocketSession session);

    // 处理离开房间（包含断开连接的处理）
    Result<?> handleLeaveRoom(Long meetingId, Long userId);

    // 处理音频流状态变更
    Result<?> handleAudioStreamChange(Long meetingId, Long userId, boolean isEnabled);

    // 转发WebRTC信令
    void forwardSignal(Long meetingId, Long fromUserId, Long toUserId, Map<String, Object> signal);

    // 获取ICE服务器配置
    Map<String, Object> getIceServers();
    // 处理 SDP offer
    void handleOffer(Long meetingId, Long fromUserId, Long toUserId, String sdp);

    // 处理 SDP answer
    void handleAnswer(Long meetingId, Long fromUserId, Long toUserId, String sdp);

    // 处理 ICE candidate
    void handleIceCandidate(Long meetingId, Long fromUserId, Long toUserId, Map<String, Object> candidate);

}