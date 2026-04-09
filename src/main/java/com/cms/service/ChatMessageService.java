package com.cms.service;

import com.cms.common.Result;

public interface ChatMessageService {
    Result<?> getMeetingChatMessages(Long meetingId, String messageType);
}