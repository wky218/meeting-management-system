package com.cms.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class WebRTCSignalEvent extends ApplicationEvent {
    private final String meetingId;
    private final String targetUserId;
    private final Map<String, Object> signalMessage;

    public WebRTCSignalEvent(String meetingId, String targetUserId, Map<String, Object> signalMessage) {
        super(signalMessage);
        this.meetingId = meetingId;
        this.targetUserId = targetUserId;
        this.signalMessage = signalMessage;
    }
}