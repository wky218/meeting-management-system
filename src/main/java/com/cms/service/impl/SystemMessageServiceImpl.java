package com.cms.service.impl;

import com.cms.pojo.ChatMessage;
import com.cms.mapper.ChatMessageMapper;
import com.cms.service.SystemMessageService;
import com.cms.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.core.JsonProcessingException; // 导入 JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 确保Slf4j导入正确
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap; // 导入 HashMap
import java.util.List; // 导入 List
import java.util.Map; // 导入 Map

@Slf4j // 使用 Lombok 的 @Slf4j 自动生成 log 字段
@Service
@RequiredArgsConstructor // Lombok 生成包含所有 final 字段的构造函数
public class SystemMessageServiceImpl implements SystemMessageService {

    // 移除手动声明的 logger 字段，因为 @Slf4j 会生成
    // private static final Logger log = LoggerFactory.getLogger(SystemMessageServiceImpl.class);

    private final ChatMessageMapper chatMessageMapper;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    // 如果使用了 @RequiredArgsConstructor，通常不需要手动添加 @Autowired 构造函数
    // 但为了明确依赖，保留手动构造函数也是可以的，但要注意可能与 @RequiredArgsConstructor 冲突的警告（虽然通常不会导致编译错误）
    // @Autowired
    // public SystemMessageServiceImpl(ChatMessageMapper chatMessageMapper, WebSocketSessionManager sessionManager, ObjectMapper objectMapper) {
    //     this.chatMessageMapper = chatMessageMapper;
    //     this.sessionManager = sessionManager;
    //     this.objectMapper = objectMapper;
    // }

    @Override
    public void sendSystemMessage(Long meetingId, String content) {
        // 检查是否是WebRTC信令消息
        if (content.startsWith("webrtc_signal:") || content.contains("\"type\":\"webrtc_")) {
            // 如果是WebRTC信令消息，只转发不存储
            TextMessage message = new TextMessage(content);
            List<WebSocketSession> sessions = sessionManager.getSessionsByMeetingId(String.valueOf(meetingId));
            if (sessions != null) { // 检查 sessions 是否为 null
                for (WebSocketSession userSession : sessions) {
                    if (userSession != null && userSession.isOpen()) { // 检查 session 和其状态
                        try {
                            userSession.sendMessage(message);
                        } catch (IOException e) {
                            // 记录 userId 时需要从 attributes 中获取，确保 key 正确
                            Object userIdAttr = userSession.getAttributes().get("userId");
                            log.error("发送WebRTC信令消息失败 - userId: {}, meetingId: {}", userIdAttr, meetingId, e);
                        }
                    }
                }
            } else {
                log.warn("未找到会议 {} 的 WebSocket 会话用于发送 WebRTC 信令消息", meetingId);
            }
            return; // 直接返回，不存储到数据库
        }

        // 处理普通系统消息
        ChatMessage message = new ChatMessage();
        message.setMeetingId(meetingId);
        message.setUserId(0L); // 系统消息 user_id 设置为 0
        message.setContent(content);
        message.setSendTime(LocalDateTime.now());
        message.setMessageType("SYSTEM");
        message.setSenderName("系统"); // 设置 senderName 为“系统”
        message.setUpdateTime(LocalDateTime.now()); // 设置 updateTime

        try {
            // 保存消息
            chatMessageMapper.insert(message);
            log.info("系统消息已保存到数据库: meetingId:{}, content:{}", meetingId, content);

            // --- 添加推送逻辑 ---
            TextMessage textMessage;
            try {
                // 将 ChatMessage 对象序列化为 JSON 字符串
                Map<String, Object> messageToSend = new HashMap<>();
                messageToSend.put("type", "SYSTEM_MESSAGE"); // 定义一个消息类型，客户端识别
                messageToSend.put("meetingId", message.getMeetingId());
                messageToSend.put("userId", message.getUserId());
                messageToSend.put("senderName", message.getSenderName());
                messageToSend.put("content", message.getContent());
                messageToSend.put("sendTime", message.getSendTime()); // 根据需要格式化时间
                // 添加其他需要发送的字段

                String messageJson = objectMapper.writeValueAsString(messageToSend);
                textMessage = new TextMessage(messageJson);
            } catch (JsonProcessingException e) {
                log.error("序列化系统消息失败: meetingId:{}, content:{}", meetingId, content, e);
                // 如果序列化失败，可以选择不发送或发送一个错误提示
                return; // 序列化失败则停止发送
            }

            // 获取会议中的所有 WebSocket 会话
            List<WebSocketSession> sessions = sessionManager.getSessionsByMeetingId(String.valueOf(meetingId));

            if (sessions != null) {
                // 遍历会话并发送消息
                for (WebSocketSession userSession : sessions) {
                    if (userSession != null && userSession.isOpen()) { // 检查 session 和其状态
                        try {
                            userSession.sendMessage(textMessage);
                            // 记录 userId 时需要从 attributes 中获取
                            Object userIdAttr = userSession.getAttributes().get("userId");
                            log.debug("系统消息已发送至用户: userId: {}, meetingId: {}", userIdAttr, meetingId);
                        } catch (IOException e) {
                            // 记录 userId 时需要从 attributes 中获取
                            Object userIdAttr = userSession.getAttributes().get("userId");
                            log.error("发送系统消息失败 - userId: {}, meetingId: {}", userIdAttr, meetingId, e);
                        }
                    }
                }
                log.info("系统消息已推送至会议中的所有用户: meetingId: {}", meetingId);
            } else {
                log.warn("未找到会议 {} 的 WebSocket 会话用于发送系统消息", meetingId);
            }
            // --- 推送逻辑结束 ---

        } catch (Exception e) {
            log.error("处理系统消息失败: meetingId:{}, content:{}", meetingId, content, e);
            // 异常处理
        }
    }
}