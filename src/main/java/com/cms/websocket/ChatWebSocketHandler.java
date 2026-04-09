package com.cms.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.enums.ParticipantRole;
import jakarta.annotation.PreDestroy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import com.cms.mapper.*;
import com.cms.pojo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import com.cms.mapper.MeetingParticipantMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler implements ApplicationContextAware{
    // 依赖注入 (使用 final 关键字，让 Lombok 自动注入)
    private final UserMapper userMapper;
    private final MeetingMapper meetingMapper; // 保留，如果用于会议验证
    private final ChatMessageMapper chatMessageMapper; // 保留，如果用于聊天消息
    private final MeetingParticipationMapper participationMapper; // 保留，如果用于参会记录
    private static ApplicationContext applicationContext;
    private final MeetingParticipantMapper meetingParticipantMapper;
    @Autowired // 保持 Autowired，如果 VoiceChatServiceImpl 依赖它
    private WebSocketSessionManager sessionManager; // 注意：如果不再需要，可以移除此字段和注入

    // 静态变量
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    // === 引入线程池和队列处理逻辑 ===
    // 解析消息线程池 (单线程)
    private static final ScheduledExecutorService executorReceive = Executors.newSingleThreadScheduledExecutor();
    // 发送消息线程池 (单线程)
    private static final ScheduledExecutorService executorSend = Executors.newSingleThreadScheduledExecutor();
    private static final ExecutorService dbOperationExecutor = Executors.newFixedThreadPool(2); // 例如，使用固定大小为2的线程池
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    // 标识线程是否已启动
    private static volatile boolean receiveThreadStarted = false;
    private static volatile boolean sendThreadStarted = false;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        ChatWebSocketHandler.applicationContext = applicationContext;
    }

    // 解析消息线程，负责从 receiveQueue 中取出消息进行解析和业务处理
    private static final Runnable receiveRunnable = new Runnable() {
        @Override
        public void run() {
            log.info("接收消息处理线程启动");
            while (true) {
                WebSocketMessageQueue webSocketMsg = WebSocketMessageQueueManager.getReceiveQueue().poll();
                if (webSocketMsg != null) {
                    try {
                        ChatWebSocketHandler handler = applicationContext.getBean(ChatWebSocketHandler.class);
                        handler.handleReceivedMessage(webSocketMsg.getSession(), webSocketMsg.getMessage());
                    } catch (Exception e) {
                        log.error("处理接收队列消息异常", e);
                        try {
                            // 这里的 session 也可能已经失效，需要谨慎处理
                            WebSocketSession session = webSocketMsg.getSession();
                            if (session != null && session.isOpen()) {
                                session.close(CloseStatus.SERVER_ERROR);
                            }
                        } catch (IOException closeException) {
                            log.error("关闭异常会话失败", closeException);
                        }
                    }
                } else {
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("接收消息处理线程被中断");
                        break;
                    }
                }
            }
            log.info("接收消息处理线程停止");
        }
    };

    // 发送消息线程，负责从 sendQueue 中取出消息进行发送
    private static final Runnable sendRunnable = new Runnable() {
        @Override
        public void run() {
            log.info("发送消息处理线程启动");
            while (true) {
                WebSocketMessageQueue webSocketMsg = WebSocketMessageQueueManager.getSendQueue().poll();
                if (webSocketMsg != null) {
                    try {
                        webSocketMsg.send();
                    } catch (IOException e) {
                        // 错误信息已经在 WebSocketMessageQueue.send() 中记录
                    } catch (Exception e) {
                        log.error("处理发送队列消息未知异常", e);
                    }
                } else {
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("发送消息处理线程被中断");
                        break;
                    }
                }
            }
            log.info("发送消息处理线程停止");
        }
    };

    // <<<<<<<<<< 使用 @PostConstruct 方法，在依赖注入完成后启动线程 >>>>>>>>>>
    @PostConstruct
    public void init() {
        startQueueProcessingThreads();
    }

    private static synchronized void startQueueProcessingThreads() {
        if (!receiveThreadStarted) {
            executorReceive.submit(receiveRunnable);
            receiveThreadStarted = true;
        }
        if (!sendThreadStarted) {
            executorSend.submit(sendRunnable);
            sendThreadStarted = true;
        }
    }


    // 提供获取会议在线用户ID列表的方法 (供 VoiceChatServiceImpl 调用)
    // 这个方法调用 WebSocketMessageQueueManager 中的对应方法
    public static List<String> getConnectedUserIds(Long meetingId) {
        if (meetingId == null) {
            return Collections.emptyList();
        }
        return WebSocketMessageQueueManager.getConnectedUserIds(meetingId);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String meetingId = null;
        String userId = null;

        try {
            String query = session.getUri().getQuery();
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1) {
                        params.put(pair[0], pair[1]);
                    } else {
                        params.put(pair[0], "");
                    }
                }
            }
            meetingId = params.get("meetingId");
            userId = params.get("userId");
            // 增加日志记录
            log.info("提取的 meetingId: {}, userId: {}", meetingId, userId);

            Long meetingIdLong = null;
            Long userIdLong = null;

            if (meetingId != null && userId != null) {
                meetingIdLong = Long.parseLong(meetingId);
                userIdLong = Long.parseLong(userId);

                // <<<<<<<<<< 在这里显式地将 meetingId 和 userId 添加到会话属性中 >>>>>>>>>>
                // 这些属性在 afterConnectionClosed 等方法中会用到，需要同步设置
                session.getAttributes().put("meetingId", meetingId);
                session.getAttributes().put("userId", userId);
                // 增加日志记录
                log.info("会话属性设置完成 - meetingId: {}, userId: {}", meetingId, userId);

            } else {
                log.error("WebSocket连接建立失败：缺少 meetingId 或 userId 参数");
                session.close(CloseStatus.POLICY_VIOLATION.withReason("缺少必要参数"));
                return;
            }

            // 验证用户和会议是否存在 - 这些通常是快速读取操作，可以保留在握手线程
            User user = userMapper.selectById(userIdLong);
            Meeting meeting = meetingMapper.selectById(meetingIdLong);

            if (user == null || meeting == null) {
                log.error("用户或会议不存在 - userId: {}, meetingId: {}", userId, meetingId);
                session.close(CloseStatus.POLICY_VIOLATION.withReason("用户或会议不存在"));
                return;
            }

            // 处理现有连接 - 这部分逻辑也需要同步执行
            handleExistingConnection(userId, session);

            // 保存新的会话信息到队列管理器 - 这部分也需要同步执行，以便后续消息能路由到新会话
            // 现在 session 对象已经包含了 meetingId 和 userId 属性
            WebSocketMessageQueueManager.addSession(meetingId, userId, session);

            // <<<<<<<<<< 将后续的数据库写入操作和系统消息发送提交到新的线程池异步执行 >>>>>>>>>>
            // 将必要的变量作为 final 传递给 lambda
            final Long finalMeetingIdLong = meetingIdLong;
            final Long finalUserIdLong = userIdLong;
            final String finalMeetingId = meetingId;
            final String finalUserId = userId;
            final String finalUsername = user.getUsername(); // 获取用户名传递过去

            dbOperationExecutor.submit(() -> {
                try {
                    // 异步更新参会记录 (包括可能的插入操作)
                    updateParticipationRecord(finalMeetingIdLong, finalUserIdLong);

                    // 异步发送系统加入消息 (保存消息到数据库并广播)
                    ChatMessage joinMessage = new ChatMessage();
                    joinMessage.setMeetingId(finalMeetingIdLong);
                    joinMessage.setUserId(finalUserIdLong); // 使用 Long 类型
                    joinMessage.setContent(String.format("userId:%s, username:%s, action:加入会议",
                            finalUserId, finalUsername)); // 使用传递的用户名
                    joinMessage.setSendTime(LocalDateTime.now());
                    joinMessage.setSenderName("系统");
                    joinMessage.setMessageType("SYSTEM");

                    // <<<<<<<<<< 异步插入数据库 >>>>>>>>>>
                    chatMessageMapper.insert(joinMessage);
                    log.info("异步处理：系统加入消息已保存到数据库: meetingId:{}, content:{}", finalMeetingIdLong, joinMessage.getContent());

                    // <<<<<<<<<< 异步广播加入消息 >>>>>>>>>>
                    String broadcastMessageContent = objectMapper.writeValueAsString(joinMessage);
                    WebSocketMessageQueueManager.broadcastMessageToSendQueue(finalMeetingId, broadcastMessageContent);
                    log.info("异步处理：系统加入消息已放入广播队列: meetingId:{}, content:{}", finalMeetingId, joinMessage.getContent());


                } catch (Exception e) {
                    log.error("异步处理连接建立后的数据库操作或消息发送失败 - meetingId: {}, userId: {}", finalMeetingId, finalUserId, e);
                    // 可以在这里处理异步任务中的异常，例如记录更详细的错误信息
                }
            });


            // 移除 WebRTC 初始化连接逻辑 (如果之前有的话)

            // <<<<<<<<<< 重要的点：在 afterConnectionEstablished 方法的主流程中，这些数据库操作不再阻塞 >>>>>>>>>>
            // 这个日志会在异步任务开始后很快打印出来，表示握手线程已经完成主要工作
            log.info("用户WebSocket连接建立完成 (后续数据库操作和消息发送异步执行) - userId: {}, meetingId: {}",
                    userId, meetingId);

            // afterConnectionEstablished 方法尽快返回，释放握手线程
        } catch (Exception e) {
            log.error("WebSocket连接建立主流程失败 - meetingId: {}, userId: {}", meetingId, userId, e);
            try {
                // 确保在主流程发生异常时关闭会话
                if (session != null && session.isOpen()) {
                    session.close(CloseStatus.SERVER_ERROR.withReason("服务器内部错误"));
                }
            } catch (IOException ex) {
                log.error("关闭WebSocket连接失败", ex);
            }
        }
    }
    // 修改 handleExistingConnection 方法，使用 WebSocketMessageQueueManager 中的 Map
    private void handleExistingConnection(String userId, WebSocketSession newSession) throws IOException {
        WebSocketSession existingSession = WebSocketMessageQueueManager.getUserSession(userId);

        if (existingSession != null && existingSession.isOpen() && !existingSession.getId().equals(newSession.getId())) {
            String oldMeetingId = (String) existingSession.getAttributes().get("meetingId");
            existingSession.close(CloseStatus.POLICY_VIOLATION.withReason("在其他地方重新连接"));
            if (oldMeetingId != null) {
                WebSocketMessageQueueManager.removeSession(oldMeetingId, userId);
            }
        }
    }

    private void updateParticipationRecord(Long meetingId, Long userId) {
        try {
            LambdaQueryWrapper<MeetingParticipant> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                    .eq(MeetingParticipant::getUserId, userId);

            MeetingParticipant participant = meetingParticipantMapper.selectOne(queryWrapper);

            if (participant != null) {
                // 如果找到记录，更新最后活跃时间
                participant.setLastActiveTime(new Date()); // 使用 java.util.Date
                meetingParticipantMapper.updateById(participant);
                log.debug("更新参会记录 lastActiveTime - meetingId: {}, userId: {}", meetingId, userId);
            } else {
                // 如果未找到记录，插入一条新的参会记录
                log.warn("未找到对应的参会记录，将插入新记录 - meetingId: {}, userId: {}", meetingId, userId);
                MeetingParticipant newParticipant = new MeetingParticipant();
                // 请确保 MeetingParticipant 的其他非空字段在这里被设置，例如 role，可以设置一个默认值
                newParticipant.setMeetingId(meetingId);
                newParticipant.setUserId(userId);
                newParticipant.setSignInTime(new Date()); // 设置签到时间（可以根据业务需求调整）
                newParticipant.setLastActiveTime(new Date()); // 设置最后活跃时间
                newParticipant.setRole(ParticipantRole.PARTICIPANT); // 设置默认角色，确保 ParticipantRole 枚举可用
                newParticipant.setSignInStatus("已签到"); // 设置默认签到状态
                newParticipant.setLeaveStatus("正常"); // 设置默认请假状态
                newParticipant.setMuted(false); // 设置默认静音状态

                meetingParticipantMapper.insert(newParticipant);
                log.info("插入新的参会记录成功 - meetingId: {}, userId: {}", meetingId, userId);
            }
        } catch (Exception e) {
            log.error("处理参会记录失败 (更新或插入) - meetingId: {}, userId: {}", meetingId, userId, e);
        }
    }

    // 移除 WebRTC 初始化连接方法




    // TODO: handleMessage 方法仅将消息放入接收队列，实际处理在 receiveRunnable 中
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WebSocketMessageQueueManager.putMessageToReceiveQueue(session, message);
    }

    // 新增一个方法来处理从接收队列中取出的消息 (移除 WebRTC 处理逻辑)
    private void handleReceivedMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String meetingId = (String) session.getAttributes().get("meetingId");
        String userId = (String) session.getAttributes().get("userId");
        // 增加日志记录
        log.info("处理接收消息时提取的 meetingId: {}, userId: {}", meetingId, userId);

        if (meetingId == null || userId == null) {
            log.error("处理接收消息时，会话信息不完整");
            return;
        }

        String content = ((TextMessage) message).getPayload().trim();
        if (content.isEmpty()) {
            return;
        }

        try {
            User user = userMapper.selectById(Long.parseLong(userId));
            String username = user != null ? user.getUsername() : "未知用户";

            // 处理其他消息（例如聊天消息）
            handleChatMessage(meetingId, userId, content, username);

        } catch (Exception e) {
            log.error("处理接收到的消息失败", e);
        }
    }


    private void handleChatMessage(String meetingId, String userId, String content, String username) {
        try {
            // 检查是否是WebRTC信令消息 (保留检查，但移除转发逻辑)
            if (content.startsWith("webrtc_signal:") || content.contains("\"type\":\"webrtc_")) {
                log.warn("接收到 WebRTC 信令消息，不处理和转发: {}", content);
                return;
            }

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMeetingId(Long.parseLong(meetingId));
            chatMessage.setUserId(Long.parseLong(userId));
            chatMessage.setContent(content);
            chatMessage.setSendTime(LocalDateTime.now());
            chatMessage.setSenderName(username);
            chatMessage.setMessageType("CHAT");

            chatMessageMapper.insert(chatMessage);

            // 广播消息 (通过队列广播)
            broadcastMessage(meetingId, chatMessage);
        } catch (Exception e) {
            log.error("处理聊天消息失败 - meetingId: {}, userId: {}", meetingId, userId, e);
        }
    }
    // 广播消息给房间内所有用户 (已修改为使用队列，保留)
    public void broadcastMessage(String meetingId, ChatMessage message) {
        try {
            String broadcastMessageContent = objectMapper.writeValueAsString(message);
            WebSocketMessageQueueManager.broadcastMessageToSendQueue(meetingId, broadcastMessageContent);
        } catch (Exception e) {
            log.error("外部调用广播消息失败 - meetingId: {}", meetingId, e);
        }
    }


    // 修改 afterConnectionClosed 方法
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // updateParticipationOnLeave 方法也执行数据库操作
        // 如果这个操作也可能导致断开连接线程的延迟，也可以考虑将其异步化，类似 afterConnectionEstablished 中的处理
        // 但通常断开连接的优先级没有建立连接那么高，而且 afterConnectionClosed 本身不应该阻塞太久
        // 所以是否异步化 updateParticipationOnLeave 取决于实际测试和性能需求
        String meetingId = (String) session.getAttributes().get("meetingId");
        String userId = (String) session.getAttributes().get("userId");

        // 增加日志记录
        log.info("会话关闭时提取的 meetingId: {}, userId: {}", meetingId, userId);

        try {
            if (meetingId == null || userId == null) {
                log.warn("会话关闭时未找到会议ID或用户ID");
                return;
            }

            // 移除会话 (使用队列管理器的方法) - 这部分需要同步，以确保及时从在线列表中移除
            WebSocketMessageQueueManager.removeSession(meetingId, userId);

            Long meetingIdLong = null;
            Long userIdLong = null;
            if (meetingId != null && userId != null) {
                meetingIdLong = Long.parseLong(meetingId);
                userIdLong = Long.parseLong(userId);
            }

            // 更新参会记录 (使用 MeetingParticipantMapper) - 可以在这里异步化
            if (meetingIdLong != null && userIdLong != null) {
                // 可以在这里提交到 dbOperationExecutor 或另一个线程池
                // dbOperationExecutor.submit(() -> {
                updateParticipationOnLeave(meetingIdLong, userIdLong);
                // });
            }
            // 发送离开消息 (通过队列发送) - 可以保留在 afterConnectionClosed 线程，或者也异步化
            // 如果离开消息的发送对及时性要求不高，可以异步化
            // dbOperationExecutor.submit(() -> {
            User user = userMapper.selectById(userIdLong);
            String username = user != null ? user.getUsername() : "未知用户";

            ChatMessage leaveMessage = new ChatMessage();
            leaveMessage.setMeetingId(meetingIdLong);
            leaveMessage.setUserId(userIdLong); // 使用 Long 类型
            leaveMessage.setContent(String.format("userId:%s, username:%s, action:离开会议",
                    userId, username));
            leaveMessage.setSendTime(LocalDateTime.now());
            leaveMessage.setSenderName("系统");
            leaveMessage.setMessageType("SYSTEM");

            // chatMessageMapper.insert(leaveMessage); // <<<<<<<<<< 异步插入数据库 (如果异步化)

             WebSocketMessageQueueManager.broadcastMessageToSendQueue(meetingId, objectMapper.writeValueAsString(leaveMessage)); // <<<<<<<<<< 异步广播 (如果异步化)

            // }); // 异步任务结束


            log.info("用户离开会议处理完成 - userId: {}, meetingId: {}", userId, meetingId);

        } catch (Exception e) {
            log.error("处理连接关闭时发生错误 - userId: {}, meetingId: {}", userId, meetingId, e);
        }
    }

    /**
     * 更新参会记录：用户断开 WebSocket 时调用，更新最后活跃时间或标记为离线
     * @param meetingId 会议ID
     * @param userId 用户ID
     */
    private void updateParticipationOnLeave(Long meetingId, Long userId) {
        try {
            LambdaQueryWrapper<MeetingParticipant> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                    .eq(MeetingParticipant::getUserId, userId);
            // 使用 MeetingParticipantMapper 来查询 MeetingParticipant
            MeetingParticipant participant = meetingParticipantMapper.selectOne(queryWrapper); // <<<<<<<<<< 修改这里

            if (participant != null) {
                // 这里可以选择更新 lastActiveTime 或设置一个离线状态
                // 考虑到 getOnlineParticipants 是基于 lastActiveTime 的，这里也更新 lastActiveTime
                participant.setLastActiveTime(new Date()); // 使用 java.util.Date
                // 如果 MeetingParticipant 有一个表示在线状态的字段，可以在这里设置为离线
                // participant.setStatus("OFFLINE");
                // 使用 MeetingParticipantMapper 来更新 MeetingParticipant
                meetingParticipantMapper.updateById(participant); // <<<<<<<<<< 修改这里
                log.debug("更新参会记录 lastActiveTime (用户离开) - meetingId: {}, userId: {}", meetingId, userId);
            } else {
                log.warn("未找到对应的参会记录，无法更新 lastActiveTime (用户离开) - meetingId: {}, userId: {}", meetingId, userId);
            }
        } catch (Exception e) {
            log.error("更新参会记录失败 (用户离开) - meetingId: {}, userId: {}", meetingId, userId, e);
        }
    }

    // 修改 handleTransportError 方法 (移除 WebRTC 相关逻辑)
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = null;
        String meetingIdStr = null;
        try {
            Map<String, Object> attributes = session.getAttributes();
            // 获取 userId 对象，可能是 String 类型
            Object userIdObj = attributes.get("userId");
            if (userIdObj != null) {
                // 将对象转换为 String，然后解析为 Long
                userId = Long.parseLong(userIdObj.toString());
            }

            String uri = session.getUri() != null ? session.getUri().getPath() : "";
            if (uri.startsWith("/websocket/chat/")) {
                meetingIdStr = uri.substring("/websocket/chat/".length());
            }
            logger.error("WebSocket传输错误 - userId: {}, meetingId: {}", userId, meetingIdStr, exception);
        } catch (Exception e) {
            // 捕获处理错误时可能发生的异常，例如解析 Long 失败
            logger.error("Error handling transport error for session: {}", session.getId(), e);
        } finally {
            try {
                // 在传输错误发生后关闭 session
                if (session.isOpen()) {
                    session.close(CloseStatus.SERVER_ERROR);
                }
            } catch (IOException e) {
                logger.error("Error closing session after transport error: {}", session.getId(), e);
            }
        }
    }
    // <<<<<<<<<< 关闭线程池 >>>>>>>>>>
    @PreDestroy // 导入 @PreDestroy
    public void destroy() {
        log.info("关闭 WebSocket 线程池...");
        executorReceive.shutdown();
        executorSend.shutdown();
        dbOperationExecutor.shutdown();
        try {
            if (!executorReceive.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("接收线程池未能及时关闭");
            }
            if (!executorSend.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("发送线程池未能及时关闭");
            }
            if (!dbOperationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("数据库操作线程池未能及时关闭");
            }
        } catch (InterruptedException e) {
            log.warn("关闭线程池时被中断", e);
        }
        log.info("WebSocket 线程池已关闭.");
    }
}