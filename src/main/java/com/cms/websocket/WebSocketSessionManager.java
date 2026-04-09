package com.cms.websocket;

import com.cms.websocket.message.MuteMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class WebSocketSessionManager {
    // 使用ConcurrentHashMap存储会议ID和对应的WebSocket会话列表
    private final ConcurrentHashMap<String, Set<WebSocketSession>> meetingSessions = new ConcurrentHashMap<>();

    // 添加会话
    public void addSession(String meetingId, WebSocketSession session) {
        meetingSessions.computeIfAbsent(meetingId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("新的WebSocket连接已建立 - meetingId: {}, sessionId: {}", meetingId, session.getId());
    }

    // 移除会话
    public void removeSession(String meetingId, WebSocketSession session) {
        Set<WebSocketSession> sessions = meetingSessions.get(meetingId);
        if (sessions != null) {
            // 获取用户ID
            String userId = (String) session.getAttributes().get("userId");
            log.debug("尝试移除会话 - meetingId: {}, userId: {}", meetingId, userId);

            if (sessions.remove(session)) {
                if (sessions.isEmpty()) {
                    meetingSessions.remove(meetingId);
                }
                log.info("WebSocket连接已断开 - meetingId: {}, userId: {}, sessionId: {}",
                        meetingId,
                        userId,
                        session.getId()
                );
            } else {
                log.warn("移除会话失败 - meetingId: {}, userId: {}", meetingId, userId);
            }
        } else {
            log.warn("未找到会话集合 - meetingId: {}", meetingId);
        }
    }

    // 获取会议的所有会话
    public List<WebSocketSession> getSessionsByMeetingId(String meetingId) {
        Set<WebSocketSession> sessions = meetingSessions.get(meetingId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("未找到会议的会话 - meetingId: {}", meetingId);
            return new ArrayList<>();
        }
        log.debug("获取到会议的会话数量 - meetingId: {}, count: {}", meetingId, sessions.size());
        return new ArrayList<>(sessions);
    }

    // 发送消息给会议中的所有用户
    public void broadcastToMeeting(Long meetingId, WebSocketMessage<?> message) {
        String meetingIdStr = String.valueOf(meetingId);
        Set<WebSocketSession> sessions = meetingSessions.get(meetingIdStr);
        if (sessions == null || sessions.isEmpty()) {
            log.warn("未找到会议的会话 - meetingId: {}", meetingId);
            return;
        }

        // 将MuteMessage转换为TextMessage
        TextMessage textMessage;
        if (message instanceof MuteMessage muteMessage) {
            textMessage = new TextMessage(muteMessage.getPayload());
        } else if (message instanceof TextMessage) {
            textMessage = (TextMessage) message;
        } else {
            log.error("不支持的消息类型: {}", message.getClass().getName());
            return;
        }

        sessions.stream()
                .filter(WebSocketSession::isOpen)
                .forEach(session -> {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.error("发送消息失败 - meetingId: {}, sessionId: {}",
                                meetingId, session.getId(), e);
                    }
                });
    }
    // 发送消息给会议中的指定用户
    public void sendToUser(String meetingId, String targetUserId, TextMessage message) {
        List<WebSocketSession> sessions = getSessionsByMeetingId(meetingId);
        for (WebSocketSession session : sessions) {
            try {
                String userId = (String) session.getAttributes().get("userId");
                if (session.isOpen() && targetUserId.equals(userId)) {
                    session.sendMessage(message);
                    break;
                }
            } catch (IOException e) {
                log.error("发送消息给用户失败 - meetingId: {}, userId: {}", meetingId, targetUserId, e);
            }
        }
    }

    public boolean isUserInMeeting(Long meetingId, Long userId) {
        Set<WebSocketSession> sessions = meetingSessions.get(String.valueOf(meetingId));
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }

        return sessions.stream()
                .filter(WebSocketSession::isOpen)
                .anyMatch(session -> {
                    String sessionUserId = (String) session.getAttributes().get("userId");
                    return String.valueOf(userId).equals(sessionUserId);
                });
    }

//    public List<String> getConnectedUserIds(String meetingId) {
//        List<WebSocketSession> sessions = getSessionsByMeetingId(meetingId);
//        return sessions.stream()
//                .filter(WebSocketSession::isOpen)
//                .map(session -> (String) session.getAttributes().get("userId"))
//                .distinct()
//                .collect(Collectors.toList());
//    }
public List<String> getConnectedUserIds(Long meetingId) {
    Set<WebSocketSession> sessions = meetingSessions.get(meetingId);
    if (sessions == null || sessions.isEmpty()) {
        log.debug("未找到会议的会话 - meetingId: {}", meetingId);
        return new ArrayList<>();
    }

    return sessions.stream()
            .filter(WebSocketSession::isOpen)  // 只获取开启的会话
            .map(session -> {
                try {
                    // 从会话属性中获取userId
                    String userId = (String) session.getAttributes().get("userId");
                    if (userId == null) {
                        log.warn("会话中未找到用户ID - sessionId: {}", session.getId());
                    }
                    return userId;
                } catch (Exception e) {
                    log.error("获取会话用户ID失败 - sessionId: {}", session.getId(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)  // 过滤掉null值
            .distinct()  // 去重
            .collect(Collectors.toList());
}
}