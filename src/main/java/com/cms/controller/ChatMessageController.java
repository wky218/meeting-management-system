package com.cms.controller;

import com.cms.common.Result;
import com.cms.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/messages/{meetingId}")
    public Result<?> getMeetingMessages(@PathVariable Long meetingId) {
        // 使用 getMeetingChatMessages 方法并传递 'CHAT' 作为 messageType
        return chatMessageService.getMeetingChatMessages(meetingId, "CHAT");
    }
}