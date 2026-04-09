package com.cms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Configuration
@EnableConfigurationProperties
public class WebRTCConfig {

    @Bean
    @ConfigurationProperties(prefix = "webrtc")
    public WebRTCProperties webRTCProperties() {
        return new WebRTCProperties();
    }

    @Data
    public static class WebRTCProperties {
        // Google的公共STUN服务器
        private List<String> stunServers = new ArrayList<>(List.of(
            "stun:stun.l.google.com:19302",
            "stun:stun1.l.google.com:19302",
            "stun:stun2.l.google.com:19302",
            "stun:stun3.l.google.com:19302",
            "stun:stun4.l.google.com:19302"
        ));

        // 你的TURN服务器配置
        private String turnServer = "turn:fwwhub.fun:3478";
        private String turnUsername = "user1";
        private String turnCredential = "password1";
        private String realm = "fwwhub.fun";

        public List<Map<String, Object>> getIceServers() {
            List<Map<String, Object>> iceServers = new ArrayList<>();
            
            // 添加所有STUN服务器
            for (String stunServer : stunServers) {
                Map<String, Object> stun = new HashMap<>();
                stun.put("urls", stunServer);
                iceServers.add(stun);
            }
            
            // 添加TURN服务器
            Map<String, Object> turn = new HashMap<>();
            turn.put("urls", turnServer);
            turn.put("username", turnUsername);
            turn.put("credential", turnCredential);
            turn.put("realm", realm);
            iceServers.add(turn);
            
            return iceServers;
        }
    }
}