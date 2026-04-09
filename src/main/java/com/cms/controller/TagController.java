package com.cms.controller;

import com.cms.common.Result;
import com.cms.dto.MeetingTagRequest;
import com.cms.pojo.Tag;
import com.cms.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    // 标签基础操作
    @PostMapping
    public Result<?> createTag(@RequestBody Tag tag) {
        if (tag == null || tag.getTagName() == null || tag.getTagName().trim().isEmpty()) {
            return Result.error("标签名称不能为空");
        }
        return tagService.createTag(tag.getTagName());
    }
    @DeleteMapping("/{tagId}")
    public Result<?> deleteTag(@PathVariable Long tagId) {
        return tagService.deleteTag(tagId);
    }

    @GetMapping
    public Result<?> getAllTags() {
        return tagService.getAllTags();
    }

    // 用户标签操作
    @PostMapping("/user/{userId}")
    public Result<?> addUserTag(
            @PathVariable Long userId,
            @RequestBody MeetingTagRequest request) {
        return tagService.addUserTagByName(userId, request.getTagName());
    }
    @DeleteMapping("/user/{userId}/{tagId}")
    public Result<?> removeUserTag(
            @PathVariable Long userId,
            @PathVariable Long tagId) {
        return tagService.removeUserTag(userId, tagId);
    }

    @GetMapping("/user/{userId}")
    public Result<?> getUserTags(@PathVariable Long userId) {
        return tagService.getUserTags(userId);
    }

    // 会议标签操作
    @PostMapping("/meeting/{meetingId}")
    public Result<?> addMeetingTag(
            @PathVariable Long meetingId,
            @RequestBody MeetingTagRequest request) {
        return tagService.addMeetingTagByName(meetingId, request.getTagName());
    }
    @DeleteMapping("/meeting/{meetingId}/{tagId}")
    public Result<?> removeMeetingTag(
            @PathVariable Long meetingId,
            @PathVariable Long tagId) {
        return tagService.removeMeetingTag(meetingId, tagId);
    }

    @GetMapping("/meeting/{meetingId}")
    public Result<?> getMeetingTags(@PathVariable Long meetingId) {
        return tagService.getMeetingTags(meetingId);
    }

    // 批量操作
    @PostMapping("/meeting/{meetingId}/batch")
    public Result<?> batchAddMeetingTags(
            @PathVariable Long meetingId,
            @RequestBody List<String> tagNames) {
        return tagService.batchAddMeetingTags(meetingId, tagNames);
    }

    @PostMapping("/user/{userId}/batch")
    public Result<?> batchAddUserTags(
            @PathVariable Long userId,
            @RequestBody List<String> tagNames) {
        return tagService.batchAddUserTags(userId, tagNames);
    }
}