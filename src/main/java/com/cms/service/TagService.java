package com.cms.service;

import com.cms.common.Result;
import java.util.List;

public interface TagService {
    // 标签基础操作
    Result<?> createTag(String tagName);
    Result<?> deleteTag(Long tagId);
    Result<?> getAllTags();

    // 用户标签操作
    Result<?> addUserTagByName(Long userId, String tagName);
    Result<?> removeUserTag(Long userId, Long tagId);
    Result<?> getUserTags(Long userId);

    // 会议标签操作
    Result<?> addMeetingTagByName(Long meetingId, String tagName);

    Result<?> removeMeetingTag(Long meetingId, Long tagId);
    Result<?> getMeetingTags(Long meetingId);

    // 批量操作
    Result<?> batchAddMeetingTags(Long meetingId, List<String> tagNames);
    Result<?> batchAddUserTags(Long userId, List<String> tagNames);
}