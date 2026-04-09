package com.cms.service;

import com.cms.common.Result;

public interface MeetingRecommendService {
    /**
     * 基于用户标签推荐公开会议
     * @param userId 用户ID
     * @param limit 推荐数量限制
     * @return 推荐的会议列表
     */
    Result<?> recommendPublicMeetings(Long userId, Integer limit);

    /**
     * 获取用户的私有会议列表
     * @param userId 用户ID
     * @return 用户参与的私有会议列表
     */
    Result<?> getPrivateMeetings(Long userId);
}