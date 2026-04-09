package com.cms.service.impl;

import com.cms.exception.AIServiceException;
import com.cms.service.AIService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AIServiceImpl implements AIService {

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.secret}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private String getAccessToken() {
        String url = String.format("https://aip.baidubce.com/oauth/2.0/token?client_id=%s&client_secret=%s&grant_type=client_credentials",
                apiKey, secretKey);

        try {
            Map<String, String> response = restTemplate.getForObject(url, Map.class);
            return response.get("access_token");
        } catch (Exception e) {
            log.error("获取百度 access_token 失败: {}", e.getMessage(), e);
            throw new AIServiceException("AI服务认证失败", "AUTH_ERROR");
        }
    }

    @Override
    public String generateSummary(String content) {
        try {
            String accessToken = getAccessToken();
            String apiUrlWithToken = apiUrl + "?access_token=" + accessToken;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "user",
                    "content", "请根据以下会议内容生成一份结构清晰的会议纪要：\n" + content
            ));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);

            try {
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                Map<String, Object> response = restTemplate.postForObject(apiUrlWithToken, request, Map.class);

                if (response != null && response.containsKey("result")) {
                    return (String) response.get("result");
                }
                throw new AIServiceException("AI服务返回数据格式错误", "AI_RESPONSE_FORMAT_ERROR");
            } catch (HttpClientErrorException e) {
                log.error("调用百度 AI 服务失败: {}", e.getMessage(), e);
                throw new AIServiceException("AI服务调用失败: " + e.getMessage(), "AI_SERVICE_ERROR");
            }
        } catch (Exception e) {
            if (e instanceof AIServiceException) {
                throw e;
            }
            throw new AIServiceException("系统内部错误: " + e.getMessage(), "SYSTEM_ERROR");
        }
    }
}