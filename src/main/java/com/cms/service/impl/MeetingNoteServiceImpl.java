package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.common.Result;
import com.cms.pojo.Meeting;
import com.cms.pojo.MeetingNote;
import com.cms.pojo.User;
import com.cms.mapper.MeetingMapper;
import com.cms.mapper.MeetingNoteMapper;
import com.cms.mapper.UserMapper;
import com.cms.service.MeetingNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// ... existing code ...

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingNoteServiceImpl implements MeetingNoteService {

    private final MeetingNoteMapper meetingNoteMapper;
    private final MeetingMapper meetingMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public Result<?> createNote(Long meetingId, Long userId, String noteName, String content, Boolean isPublic) {
        // 检查会议和用户是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        User user = userMapper.selectById(userId);

        if (meeting == null || user == null) {
            return Result.error("会议或用户不存在");
        }

        // 创建笔记
        MeetingNote note = new MeetingNote();
        note.setMeetingId(meetingId);
        note.setUserId(userId);
        note.setNoteName(noteName);
        note.setNoteContent(content);
        note.setIsPublic(isPublic);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        meetingNoteMapper.insert(note);
        return Result.success(note);
    }

    @Override
    @Transactional
    public Result<?> uploadNoteFile(Long meetingId, Long userId, String noteName, MultipartFile file, Boolean isPublic) {
        try {
            // 读取文件内容
            String content = new BufferedReader(
                    new InputStreamReader(file.getInputStream()))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // 调用创建笔记方法
            return createNote(meetingId, userId, noteName, content, isPublic);
        } catch (Exception e) {
            log.error("上传笔记文件失败", e);
            return Result.error("上传笔记文件失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<?> updateNote(Long noteId, Long userId, String noteName, String content, Boolean isPublic) {
        MeetingNote note = meetingNoteMapper.selectById(noteId);
        if (note == null) {
            return Result.error("笔记不存在");
        }
        // 检查是否是笔记创建者
        if (!note.getUserId().equals(userId)) {
            return Result.error("只有笔记创建者可以修改笔记");
        }
        // 更新笔记信息
        if (noteName != null) {
            note.setNoteName(noteName);
        }
        if (content != null) {
            note.setNoteContent(content);
        }
        if (isPublic != null) {
            note.setIsPublic(isPublic);
        }
        note.setUpdatedAt(LocalDateTime.now());

        meetingNoteMapper.updateById(note);
        return Result.success("更新笔记成功");
    }

    @Override
    @Transactional
    public Result<?> deleteNote(Long noteId, Long userId) {
        MeetingNote note = meetingNoteMapper.selectById(noteId);

        if (note == null) {
            return Result.error("笔记不存在");
        }

        // 检查是否是笔记创建者
        if (!note.getUserId().equals(userId)) {
            return Result.error("只有笔记创建者可以删除笔记");
        }

        meetingNoteMapper.deleteById(noteId);
        return Result.success("删除笔记成功");
    }

    // ... existing code ...

    @Override
    public Result<?> getMeetingNotes(Long meetingId, Long userId, Boolean includePublicOnly) {
        // 检查会议是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return Result.error("会议不存在");
        }

        // 构建查询条件
        LambdaQueryWrapper<MeetingNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingNote::getMeetingId, meetingId);

        if (includePublicOnly != null && includePublicOnly) {
            // 只查询公开笔记
            wrapper.eq(MeetingNote::getIsPublic, true);
        } else {
            // 查询公开笔记和用户自己的笔记
            wrapper.and(w -> w.eq(MeetingNote::getIsPublic, true)
                    .or(userId != null, o -> o.eq(MeetingNote::getUserId, userId)));
        }

        // 按创建时间倒序排序
        wrapper.orderByDesc(MeetingNote::getCreatedAt);

        // 查询笔记列表
        List<MeetingNote> notes = meetingNoteMapper.selectList(wrapper);
        return Result.success(notes);
    }
    @Override
    public Result<?> getNoteContent(Long noteId) {
        MeetingNote note = meetingNoteMapper.selectById(noteId);
        if (note == null) {
            return Result.error("笔记不存在");
        }
        return Result.success(note);
    }
    private boolean checkUserInMeeting(Long userId, Long meetingId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return false;
        }

        // 如果是会议创建者
        if (meeting.getOrganizerId().equals(userId)) {
            return true;
        }

        // 检查用户是否在参会者列表中
        List<User> participants = meetingMapper.getMeetingParticipants(meetingId);
        return participants != null && participants.contains(userId);
    }
}