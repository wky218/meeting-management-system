package com.cms.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage; // 导入 TextMessage

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors; // 导入 Collectors

// 统一管理WebSocket消息队列和发送逻辑
public class WebSocketMessageQueueManager {
    private static final Logger log = LoggerFactory.getLogger(WebSocketMessageQueueManager.class);
    // 待解析的消息队列 (用于 ChatWebSocketHandler 处理接收到的消息)
    public static final ArrayDeque<WebSocketMessageQueue> receiveQueue = new ArrayDeque<>();

    // 待发送的消息队列 (所有需要发送的消息都放入这里)
    public static final ArrayDeque<WebSocketMessageQueue> sendQueue = new ArrayDeque<>();

    // 用于存储用户ID到会话的映射
    private static final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>(); // 存储用户ID到会话的映射

    // 用于存储会议ID到用户ID集合的映射（使用线程安全的Set）
    // <<<<<<<<<< 修改这里，使用 Set<String> >>>>>>>>>>
    private static final Map<String, Set<String>> meetingUsers = new ConcurrentHashMap<>(); // 存储会议ID到用户ID集合的映射

    // === 提供给 ChatWebSocketHandler 调用，用于在连接建立和关闭时更新在线用户列表 ===
    public static void addSession(String meetingId, String userId, WebSocketSession session) {
        userSessions.put(userId, session);
        // <<<<<<<<<< 确保这里使用的是 ConcurrentHashMap.newKeySet() >>>>>>>>>>
        // 现在 Map 的声明类型是 Set<String>，与这里创建的 Set 类型一致
        meetingUsers.computeIfAbsent(meetingId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    public static void removeSession(String meetingId, String userId) {
        userSessions.remove(userId);
        // <<<<<<<<<< 修改这里，直接从 Set 中移除 >>>>>>>>>>
        Set<String> users = meetingUsers.get(meetingId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                meetingUsers.remove(meetingId);
            }
        }
    }

    // === 提供给外部服务调用，用于获取在线用户ID列表 (对应之前的 getConnectedUserIds) ===
    public static List<String> getConnectedUserIds(Long meetingId) {
        if (meetingId == null) {
            return Collections.emptyList();
        }
        // <<<<<<<<<< 修改这里，从 Set 转换为 List >>>>>>>>>>
        Set<String> users = meetingUsers.get(meetingId.toString());
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(users); // 或者 users.stream().collect(Collectors.toList());
    }


    // === 提供给 ChatWebSocketHandler 调用，将接收到的消息放入接收队列 ===
    public static void putMessageToReceiveQueue(WebSocketSession session, WebSocketMessage<?> message) {
        WebSocketMessageQueue item = new WebSocketMessageQueue();
        item.setSession(session);
        item.setMessage(message);
        receiveQueue.add(item);
    }

    // === 提供给 ChatWebSocketHandler 或其他服务调用，将待发送消息放入发送队列 ===

    /**
     * 将消息放入待发送队列，发送给指定用户
     * @param targetUserId 目标用户ID (String)
     * @param message 待发送消息
     */
    public static void putMessageToSendQueue(String targetUserId, WebSocketMessage<?> message) {
        WebSocketSession session = userSessions.get(targetUserId);
        if (session != null && session.isOpen()) {
            WebSocketMessageQueue item = new WebSocketMessageQueue();
            item.setSession(session);
            item.setMessage(message);
            sendQueue.add(item);
            log.debug("消息已放入发送队列，目标用户: {}", targetUserId);
        } else {
            // <<<<<<<<<< 添加日志 >>>>>>>>>>
            log.warn("未能将消息放入发送队列，目标用户 {} 的会话无效或已关闭", targetUserId);
            // <<<<<<<<<< 日志结束 >>>>>>>>>>
        }
    }

    /**
     * 将消息放入待发送队列，发送给指定用户 (TextMessage 简化版)
     * @param targetUserId 目标用户ID (String)
     * @param messageContent 待发送消息内容 (String)
     */


    /**
     * 将消息放入待发送队列，广播给会议中的所有用户
     * @param meetingId 会议ID (String)
     * @param message 待发送消息
     */
    public static void broadcastMessageToSendQueue(String meetingId, WebSocketMessage<?> message) {
        Set<String> users = meetingUsers.get(meetingId);
        // <<<<<<<<<< 添加日志 >>>>>>>>>>
        if (users != null) {
            log.info("广播消息到会议 {}，目标用户数量: {}", meetingId, users.size());
            log.debug("广播消息目标用户ID列表: {}", users);
        } else {
            log.warn("会议 {} 没有在线用户接收广播消息", meetingId);
        }
        // <<<<<<<<<< 日志结束 >>>>>>>>>>
        if (users != null) {
            for (String userId : users) {
                WebSocketSession targetSession = userSessions.get(userId);
                if (targetSession == null || !targetSession.isOpen()) {
                    log.warn("目标用户 {} 的会话无效或已关闭，跳过发送", userId);
                } else {
                    log.debug("将消息放入发送队列，目标用户: {}", userId);
                    putMessageToSendQueue(userId, message); // 这个方法内部也有日志
                }
            }
        }
    }

    /**
     * 将消息放入待发送队列，广播给会议中的所有用户 (TextMessage 简化版)
     * @param meetingId 会议ID (String)
     * @param messageContent 待发送消息内容 (String)
     */
    public static void broadcastMessageToSendQueue(String meetingId, String messageContent) {
        broadcastMessageToSendQueue(meetingId, new TextMessage(messageContent));
    }

    // === 提供给外部调用，用于获取队列实例 (如果需要更复杂的队列操作) ===
    public static ArrayDeque<WebSocketMessageQueue> getReceiveQueue() {
        return receiveQueue;
    }

    public static ArrayDeque<WebSocketMessageQueue> getSendQueue() {
        return sendQueue;
    }
    // 提供获取指定用户会话的方法
    public static WebSocketSession getUserSession(String userId) {
        return userSessions.get(userId);
    }
    /**
     * 检查用户是否在指定的会议中且WebSocket会话仍然开启
     * @param meetingId 会议ID (Long)
     * @param userId 用户ID (Long)
     * @return 如果用户在该会议中且会话开启，返回 true，否则返回 false
     */
    public static boolean isUserInMeeting(Long meetingId, Long userId) {
        if (meetingId == null || userId == null) {
            return false;
        }
        String meetingIdStr = meetingId.toString();
        String userIdStr = userId.toString();

        // 检查 meetingUsers 中是否存在该会议和用户
        // <<<<<<<<<< 现在 meetingUsers.get(meetingIdStr) 返回的是 Set<String> >>>>>>>>>>
        Set<String> usersInMeeting = meetingUsers.get(meetingIdStr);
        if (usersInMeeting == null || !usersInMeeting.contains(userIdStr)) {
            return false;
        }

        // 检查 userSessions 中是否存在该用户的有效会话并且会话是开启的
        WebSocketSession session = userSessions.get(userIdStr);
        return session != null && session.isOpen();
    }
}