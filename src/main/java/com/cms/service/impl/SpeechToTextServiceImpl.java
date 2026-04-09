package com.cms.service.impl;

import com.cms.mapper.MeetingSummaryMapper;
import com.cms.pojo.MeetingSummary;
import com.cms.service.SpeechToTextService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SpeechToTextServiceImpl implements SpeechToTextService {

    @Value("${youdao.appKey}")
    private String appKey;

    @Value("${youdao.appSecret}")
    private String appSecret;

    @Autowired
    private MeetingSummaryMapper summaryMapper;

    @Autowired
    private RestTemplate restTemplate;

    private String calculateSign(String appKey, String salt, String curtime, String input) {
        try {
            String signStr = appKey + input + salt + curtime + appSecret;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(signStr.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算签名失败", e);
            return null;
        }
    }

    @Override
    @Transactional
    public String convertToText(Long meetingId) {
        try {
            String url = "https://openapi.youdao.com/asrapi";

            String salt = String.valueOf(System.currentTimeMillis());
            String curtime = String.valueOf(System.currentTimeMillis() / 1000);

            Map<String, String> params = new HashMap<>();
            params.put("appKey", appKey);
            params.put("salt", salt);
            params.put("curtime", curtime);
            params.put("signType", "v3");
            params.put("format", "wav");
            params.put("rate", "16000");
            params.put("channel", "1");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> result = mapper.readValue(response.getBody(), Map.class);
                String text = result.get("result").toString();

                MeetingSummary summary = new MeetingSummary();
                summary.setMeetingId(meetingId);
                summary.setContent(text);
                summary.setStatus("待审核");
                summary.setMessageType("语音转写");
                summary.setCreateTime(LocalDateTime.now());
                summary.setUpdateTime(LocalDateTime.now());
                summaryMapper.insert(summary);

                return text;
            }
            return null;
        } catch (Exception e) {
            log.error("语音转文字失败: {}", e.getMessage(), e);
            return null;
        }
    }
}