package com.cms.websocket.message;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.springframework.web.socket.WebSocketMessage;

@Data
public class MuteMessage implements WebSocketMessage<String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Long userId;
    private boolean muted;
    private final String payload;

    @SneakyThrows
    public MuteMessage(Long userId, boolean muted) {
        this.userId = userId;
        this.muted = muted;
        this.payload = objectMapper.writeValueAsString(new MuteMessageContent(userId, muted));
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

    @Data
    private static class MuteMessageContent {
        private final Long userId;
        private final boolean muted;
        private final String type;

        public MuteMessageContent(Long userId, boolean muted) {
            this.userId = userId;
            this.muted = muted;
            this.type = "MUTE";  // 在构造函数中直接设置type
        }
    }
}