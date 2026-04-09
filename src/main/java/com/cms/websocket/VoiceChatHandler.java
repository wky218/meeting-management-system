package com.cms.websocket;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import java.util.HashMap;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class VoiceChatHandler implements WebSocketHandler {
    private final ObjectMapper objectMapper;
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, String> userSdp = new ConcurrentHashMap<>();
    private static final Map<String, Map<Long, WebSocketSession>> meetingParticipants = new ConcurrentHashMap<>();
    public VoiceChatHandler() {
        this.objectMapper = new ObjectMapper()
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            String meetingId = getMeetingId(session);
            Long userId = getUserId(session);

            // 验证用户权限
            if (!checkUserPermission(meetingId, userId)) {
                session.close();
                log.warn("用户 {} 无权加入会议 {}", userId, meetingId);
                return;
            }

            // 初始化会议参会者Map
            meetingParticipants.computeIfAbsent(meetingId, k -> new ConcurrentHashMap<>());
            // 添加参会者
            meetingParticipants.get(meetingId).put(userId, session);

            // 保存会话
            sessions.put(meetingId + "_" + session.getId(), session);

            // 广播参会者数量更新
            broadcastParticipantCount(meetingId);

            log.info("用户 {} 加入语音会议: {}, 当前参会人数: {}",
                    userId, meetingId, getParticipantCount(meetingId));
        } catch (Exception e) {
            log.error("建立WebSocket连接失败", e);
            closeSession(session);
        }
    }
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            String payload = message.getPayload().toString();
            log.debug("收到WebSocket消息: {}", payload);

            JsonNode node = objectMapper.readTree(payload);
            String type = node.get("type").asText();
            String meetingId = getMeetingId(session);
            String userId = getUserId(session).toString();

            log.info("处理WebSocket消息 - type: {}, meetingId: {}, userId: {}", type, meetingId, userId);

            switch (type) {
                case "offer":
                    handleOffer(session, node, meetingId, userId);
                    break;
                case "answer":
                    handleAnswer(node, meetingId);
                    break;
                case "ice-candidate":
                    handleIceCandidate(session, payload, meetingId);
                    break;
                default:
                    log.warn("未知的消息类型: {}", type);
                    session.sendMessage(new TextMessage("{\"error\": \"未知的消息类型\"}"));
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: {}", e.getMessage(), e);
            session.sendMessage(new TextMessage("{\"error\": \"消息处理失败\"}"));
        }
    }

    private void handleOffer(WebSocketSession session, JsonNode node, String meetingId, String userId) throws IOException {
        log.debug("处理Offer消息");
        String sdp = node.get("sdp").asText();
        userSdp.put(meetingId + "_" + userId, sdp);
        broadcastToOthers(session, node.toString(), meetingId);
    }

    private void handleAnswer(JsonNode node, String meetingId) throws IOException {
        log.debug("处理Answer消息");
        String targetUserId = node.get("targetUserId").asText();
        forwardToUser(meetingId, targetUserId, node.toString());
    }

    private void handleIceCandidate(WebSocketSession session, String payload, String meetingId) throws IOException {
        log.debug("处理ICE Candidate消息");
        broadcastToOthers(session, payload, meetingId);
    }
    public int getParticipantCount(String meetingId) {
        Map<Long, WebSocketSession> participants = meetingParticipants.get(meetingId);
        int count = participants != null ? participants.size() : 0;
        log.debug("获取参会人数 - meetingId: {}, count: {}", meetingId, count);
        return count;
    }
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误", exception);
        closeSession(session);
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            String meetingId = getMeetingId(session);
            Long userId = getUserId(session);

            // 移除会话
            sessions.remove(meetingId + "_" + session.getId());

            // 移除参会者并更新计数
            if (meetingParticipants.containsKey(meetingId)) {
                meetingParticipants.get(meetingId).remove(userId);
                if (meetingParticipants.get(meetingId).isEmpty()) {
                    meetingParticipants.remove(meetingId);
                }
                // 广播更新后的参会者数量
                broadcastParticipantCount(meetingId);
            }

            log.info("用户 {} 退出语音会议: {}, 当前参会人数: {}",
                    userId, meetingId, getParticipantCount(meetingId));
        } catch (Exception e) {
            log.error("关闭WebSocket连接失败", e);
        }
    }
    private void broadcastParticipantCount(String meetingId) {
        try {
            int count = getParticipantCount(meetingId);
            Map<String, Object> message = new HashMap<>();
            message.put("type", "participant-count");
            message.put("count", count);

            String payload = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(payload);

            // 向所有会议参与者广播
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen() && getMeetingId(session).equals(meetingId)) {
                    session.sendMessage(textMessage);
                }
            }

            log.debug("广播参会人数 - meetingId: {}, count: {}", meetingId, count);
        } catch (Exception e) {
            log.error("广播参会人数失败", e);
        }
    }
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private boolean checkUserPermission(String meetingId, Long userId) {
        // TODO: 实现权限检查逻辑
        return true;
    }
    private void forwardToUser(String meetingId, String userId, String message) {
        String sessionKey = meetingId + "_" + userId;
        WebSocketSession session = sessions.get(sessionKey);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.debug("消息已转发到用户 {}", userId);
            } catch (IOException e) {
                log.error("转发消息到用户{}失败: {}", userId, e.getMessage(), e);
            }
        } else {
            log.warn("用户 {} 的会话不存在或已关闭", userId);
        }
    }
    private void broadcastToOthers(WebSocketSession sender, String message, String meetingId) {
        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            if (entry.getKey().startsWith(meetingId) && !entry.getValue().getId().equals(sender.getId())) {
                try {
                    entry.getValue().sendMessage(new TextMessage(message));
                    successCount++;
                } catch (IOException e) {
                    failCount++;
                    log.error("广播消息到会话 {} 失败: {}", entry.getKey(), e.getMessage(), e);
                }
            }
        }

        log.debug("广播消息完成 - 成功: {}, 失败: {}", successCount, failCount);
    }
    private String getMeetingId(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("meetingId=")) {
                    return param.substring("meetingId=".length());
                }
            }
            throw new RuntimeException("未找到会议ID参数");
        } catch (Exception e) {
            log.error("解析会议ID失败", e);
            throw new RuntimeException("解析会议ID失败", e);
        }
    }

    private Long getUserId(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("userId=")) {
                    return Long.parseLong(param.substring("userId=".length()));
                }
            }
            throw new RuntimeException("未找到用户ID参数");
        } catch (NumberFormatException e) {
            log.error("用户ID格式错误", e);
            throw new RuntimeException("用户ID格式错误", e);
        } catch (Exception e) {
            log.error("解析用户ID失败", e);
            throw new RuntimeException("解析用户ID失败", e);
        }
    }
    private void closeSession(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
            String meetingId = getMeetingId(session);
            sessions.remove(meetingId + "_" + session.getId());
        } catch (IOException e) {
            log.error("关闭WebSocket会话失败", e);
        }
    }

}