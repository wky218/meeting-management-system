package com.cms.controller;

import com.cms.common.Result;
import com.cms.service.MeetingNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/meeting/notes")
@RequiredArgsConstructor
public class MeetingNoteController {

    private final MeetingNoteService meetingNoteService;

    @PostMapping("/create")
    public Result<?> createNote(
            @RequestParam Long meetingId,
            @RequestParam Long userId,
            @RequestParam String noteName,
            @RequestParam String content,
            @RequestParam(defaultValue = "false") Boolean isPublic) {
        return meetingNoteService.createNote(meetingId, userId, noteName, content, isPublic);
    }
    @PutMapping("/{noteId}")
    public Result<?> updateNote(
            @PathVariable Long noteId,
            @RequestParam Long userId,
            @RequestParam(required = false) String noteName,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Boolean isPublic) {  //修改可见性
        return meetingNoteService.updateNote(noteId, userId, noteName, content, isPublic);
    }

    @PostMapping("/upload")
    public Result<?> uploadNote(
            @RequestParam Long meetingId,
            @RequestParam Long userId,
            @RequestParam String noteName,
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "false") Boolean isPublic) {
        return meetingNoteService.uploadNoteFile(meetingId, userId, noteName, file, isPublic);
    }

    @DeleteMapping("/{noteId}")
    public Result<?> deleteNote(
            @PathVariable Long noteId,
            @RequestParam Long userId) {
        return meetingNoteService.deleteNote(noteId, userId);
    }

    @GetMapping("/{noteId}")
    public Result<?> getNoteContent(
            @PathVariable Long noteId) {
        return meetingNoteService.getNoteContent(noteId);
    }

    @GetMapping("/list/{meetingId}")
    public Result<?> getMeetingNotes(
            @PathVariable Long meetingId,
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "false") Boolean includePublicOnly) {
        return meetingNoteService.getMeetingNotes(meetingId, userId, includePublicOnly);
    }
}