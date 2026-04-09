package com.cms.service;

import com.cms.common.Result;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

public interface MeetingNoteService {
    @Transactional
    Result<?> createNote(Long meetingId, Long userId, String noteName, String content, Boolean isPublic);

    @Transactional
    Result<?> uploadNoteFile(Long meetingId, Long userId, String noteName, MultipartFile file, Boolean isPublic);

    @Transactional
    Result<?> updateNote(Long noteId, Long userId, String noteName, String content, Boolean isPublic);

    @Transactional
    Result<?> deleteNote(Long noteId, Long userId);

    Result<?> getMeetingNotes(Long meetingId, Long userId, Boolean includePublicOnly);
    Result<?> getNoteContent(Long noteId);
}