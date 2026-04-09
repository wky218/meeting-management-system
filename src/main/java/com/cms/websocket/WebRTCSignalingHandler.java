package com.cms.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebRTCSignalingHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    // 存储房间ID到用户集合的映射
    private final Map<String, Set<String>> roomParticipants = new ConcurrentHashMap<>();
    // 存储用户ID到会话的映射
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    // 存储会话到房间ID的映射
    private final Map<WebSocketSession, String> sessionRooms = new ConcurrentHashMap<>();

    @Data
    public static class SignalingMessage {
        private String type;        // "join", "offer", "answer", "ice-candidate", "leave"
        private String roomId;      // 房间ID
        private String sdp;         // SDP for offer/answer
        private IceCandidate candidate; // ICE candidate
        private String targetUserId;    // 目标用户ID
        private String fromUserId;      // 发送者用户ID
    }

    @Data
    public static class IceCandidate {
        private String candidate;
        private String sdpMid;
        private int sdpMLineIndex;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebRTC会话已建立 - 会话ID: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SignalingMessage signalingMessage = objectMapper.readValue(message.getPayload(), SignalingMessage.class);
        String fromUserId = extractUserId(session);
        signalingMessage.setFromUserId(fromUserId);

        switch (signalingMessage.getType()) {
            case "join":
                handleJoinRoom(session, signalingMessage);
                break;
            case "offer":
            case "answer":
            case "ice-candidate":
                forwardSignaling(signalingMessage);
                break;
            case "leave":
                handleLeaveRoom(session, signalingMessage);
                break;
            default:
                log.warn("未知的消息类型: {}", signalingMessage.getType());
        }
    }

    private void handleJoinRoom(WebSocketSession session, SignalingMessage message) throws Exception {
        String userId = message.getFromUserId();
        String roomId = message.getRoomId();
        
        // 保存会话信息
        userSessions.put(userId, session);
        sessionRooms.put(session, roomId);
        
        // 将用户添加到房间
        Set<String> participants = roomParticipants.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>());
        participants.add(userId);

        // 通知房间中的其他用户有新用户加入
        for (String participantId : participants) {
            if (!participantId.equals(userId)) {
                // 通知现有用户有新用户加入
                SignalingMessage notification = new SignalingMessage();
                notification.setType("user_joined");
                notification.setFromUserId(userId);
                notification.setRoomId(roomId);
                sendToUser(participantId, notification);

                // 通知新用户有哪些现有用户
                SignalingMessage existingUser = new SignalingMessage();
                existingUser.setType("existing_user");
                existingUser.setFromUserId(participantId);
                existingUser.setRoomId(roomId);
                sendToUser(userId, existingUser);
            }
        }

        log.info("用户 {} 加入房间 {}", userId, roomId);
    }

    private void handleLeaveRoom(WebSocketSession session, SignalingMessage message) throws Exception {
        String userId = message.getFromUserId();
        String roomId = message.getRoomId();
        
        removeUserFromRoom(userId, roomId);
        
        // 通知房间中的其他用户
        Set<String> participants = roomParticipants.get(roomId);
        if (participants != null) {
            SignalingMessage notification = new SignalingMessage();
            notification.setType("user_left");
            notification.setFromUserId(userId);
            notification.setRoomId(roomId);
            
            for (String participantId : participants) {
                sendToUser(participantId, notification);
            }
        }

        log.info("用户 {} 离开房间 {}", userId, roomId);
    }

    private void forwardSignaling(SignalingMessage message) throws Exception {
        WebSocketSession targetSession = userSessions.get(message.getTargetUserId());
        if (targetSession != null && targetSession.isOpen()) {
            String jsonMessage = objectMapper.writeValueAsString(message);
            targetSession.sendMessage(new TextMessage(jsonMessage));
            log.debug("转发{}信令 - 从用户{}到用户{}", 
                     message.getType(), 
                     message.getFromUserId(), 
                     message.getTargetUserId());
        } else {
            log.warn("目标用户{}不在线或会话已关闭", message.getTargetUserId());
        }
    }

    private void sendToUser(String userId, SignalingMessage message) throws Exception {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
        }
    }

    private void removeUserFromRoom(String userId, String roomId) {
        Set<String> participants = roomParticipants.get(roomId);
        if (participants != null) {
            participants.remove(userId);
            if (participants.isEmpty()) {
                roomParticipants.remove(roomId);
            }
        }
        
        WebSocketSession session = userSessions.get(userId);
        if (session != null) {
            sessionRooms.remove(session);
            userSessions.remove(userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        String roomId = sessionRooms.get(session);
        String userId = extractUserId(session);
        
        if (roomId != null && userId != null) {
            try {
                SignalingMessage leaveMessage = new SignalingMessage();
                leaveMessage.setType("leave");
                leaveMessage.setFromUserId(userId);
                leaveMessage.setRoomId(roomId);
                handleLeaveRoom(session, leaveMessage);
            } catch (Exception e) {
                log.error("处理连接关闭时发生错误", e);
            }
        }
        
        log.info("WebRTC会话已关闭 - 用户ID: {}", userId);
    }

    private String extractUserId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("userId=")) {
            return query.substring(7);
        }
        return session.getId();
    }
}