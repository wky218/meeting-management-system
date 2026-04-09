package com.cms.service;

import com.cms.model.UserConnection;

import java.util.List;

public interface UserConnectionService {
    /**
     * 获取用户连接信息
     * @param userId 用户ID
     * @return 用户连接信息
     */
    UserConnection getUserConnection(Long userId);

    /**
     * 更新用户连接信息
     * @param connection 连接信息
     */
    void updateUserConnection(UserConnection connection);
    List<String> getConnectedUserIds(Long meetingId);
}