package com.cms.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;

// 封装待处理的WebSocket消息，包括会话和消息本身
public class WebSocketMessageQueue {
    private static final Logger log = LoggerFactory.getLogger(WebSocketMessageQueue.class);
    private WebSocketSession session;   // socket会话信息
    private WebSocketMessage<?> message;   // 消息

    public WebSocketSession getSession() {
        return session;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }

    public WebSocketMessage<?> getMessage() {
        return message;
    }

    public void setMessage(WebSocketMessage<?> message) {
        this.message = message;
    }

    // 统一的发送消息方法，检查会话是否打开
    public void send() throws IOException {
        if (session != null && session.isOpen()) {
            session.sendMessage(message);
            // <<<<<<<<<< 成功发送日志 (可选，调试时有用) >>>>>>>>>>
            // log.debug("消息发送成功给会话: {}", session.getId());
        } else {
            // <<<<<<<<<< 添加未发送日志 >>>>>>>>>>
            String sessionId = (session != null) ? session.getId() : "null";
            boolean isOpen = (session != null) && session.isOpen();
            log.warn("消息未能发送，会话无效或已关闭 - sessionId: {}, isOpen: {}", sessionId, isOpen);
            // <<<<<<<<<< 日志结束 >>>>>>>>>>
        }
    }
}