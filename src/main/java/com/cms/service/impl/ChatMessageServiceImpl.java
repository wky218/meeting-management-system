package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cms.common.Result;
import com.cms.mapper.ChatMessageMapper;
import com.cms.pojo.ChatMessage;
import com.cms.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {
    private final ChatMessageMapper chatMessageMapper;

    @Override
    public Result<?> getMeetingChatMessages(Long meetingId, String messageType) {
        try {
            log.info("开始查询会议消息, meetingId: {}, messageType: {}", meetingId, messageType);

            QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("meeting_id", meetingId)
                    .and(wrapper -> wrapper
                            .eq(messageType != null, "message_type", messageType)
                            .or()
                            .eq("message_type", "SYSTEM"))
                    .orderByAsc("send_time");

            List<ChatMessage> messages = chatMessageMapper.selectList(queryWrapper);
            log.info("数据库查询结果: {} 条消息", messages.size());

            return Result.success(messages);
        } catch (Exception e) {
            log.error("查询会议消息失败: {}", e.getMessage(), e);
            return Result.error("获取会议消息失败: " + e.getMessage());
        }
    }
}