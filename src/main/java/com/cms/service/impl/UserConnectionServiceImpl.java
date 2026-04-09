package com.cms.service.impl;

import com.cms.common.Result;
import com.cms.model.UserConnection;
import com.cms.service.UserConnectionService;
import com.cms.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
@Slf4j
@RequiredArgsConstructor
@Service
public class UserConnectionServiceImpl implements UserConnectionService {
    // 使用内存存储用户连接信息
    private final Map<Long, UserConnection> connectionMap = new ConcurrentHashMap<>();
    @Autowired
    private WebSocketSessionManager sessionManager;
    @Override
    public UserConnection getUserConnection(Long userId) {
        return connectionMap.get(userId);
    }

    @Override
    public void updateUserConnection(UserConnection connection) {
        connectionMap.put(connection.getUserId(), connection);
    }
    @Override
    public List<String> getConnectedUserIds(Long meetingId) {
        try {
            List<String> userIds = sessionManager.getConnectedUserIds(meetingId);
            log.info("获取会议连接用户成功 - meetingId: {}, count: {}", meetingId, userIds.size());
            return userIds;
        } catch (Exception e) {
            log.error("获取会议连接用户失败 - meetingId: {}", meetingId, e);
            return new ArrayList<>();
        }
    }
}