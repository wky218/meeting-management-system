package com.cms.service;

import com.cms.pojo.BlackList;
import java.util.List;

public interface BlacklistService {
    /**
     * 将用户添加到会议黑名单
     * @param meetingId 会议ID
     * @param userId 用户ID
     * @param duration 黑名单时长（分钟），-1表示永久
     * @param operatorId 操作者ID
     * @return 是否添加成功
     */
    boolean addToBlacklist(Long meetingId, Long userId, int duration, Long operatorId);

    /**
     * 从会议黑名单中移除用户
     * @param meetingId 会议ID
     * @param userId 用户ID
     * @param operatorId 操作者ID
     * @return 是否移除成功
     */
    boolean removeFromBlacklist(Long meetingId, Long userId, Long operatorId);

    /**
     * 获取会议的黑名单列表
     * @param meetingId 会议ID
     * @return 黑名单列表
     */
    List<BlackList> getMeetingBlacklist(Long meetingId);

    /**
     * 检查用户是否在会议黑名单中
     * @param meetingId 会议ID
     * @param userId 用户ID
     * @return 是否在黑名单中
     */
    boolean isUserInBlacklist(Long meetingId, Long userId);
} 