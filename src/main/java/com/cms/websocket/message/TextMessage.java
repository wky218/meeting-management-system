package com.cms.websocket.message;

import org.springframework.web.socket.WebSocketMessage;
import lombok.Data;

@Data
public class TextMessage implements WebSocketMessage<String> {
    private final String payload;

    public TextMessage(String payload) {
        this.payload = payload;
    }

    @Override
    public String getPayload() {
        return payload;
    }

    @Override
    public int getPayloadLength() {
        return payload.length();
    }

    @Override
    public boolean isLast() {
        return true;
    }
}