package com.cms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cms.common.Result;
import com.cms.dto.SummaryGenerateDTO;
import com.cms.dto.SummaryUpdateDTO;
import com.cms.mapper.MeetingSummaryMapper;
import com.cms.pojo.ChatMessage;
import com.cms.pojo.MeetingSummary;
import com.cms.service.AIService;
import com.cms.service.ChatMessageService;
import com.cms.service.MeetingSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingSummaryServiceImpl extends ServiceImpl<MeetingSummaryMapper, MeetingSummary> implements MeetingSummaryService {

    private final ChatMessageService chatMessageService;
    private final AIService aiService;

    @Override
    public MeetingSummary generateSummary(SummaryGenerateDTO dto) {
        StringBuilder contentBuilder = new StringBuilder();

        // 获取聊天记录
        if (dto.getMessageType().equals("CHAT") || dto.getMessageType().equals("BOTH")) {
            Result<?> result = chatMessageService.getMeetingChatMessages(dto.getMeetingId(), "CHAT");
            if (result.getCode() == 200 && result.getData() != null) {
                List<ChatMessage> messages = (List<ChatMessage>) result.getData();
                for (ChatMessage message : messages) {
                    contentBuilder.append(message.getContent()).append("\n");
                }
            }
        }

        // 使用AI生成会议纪要
        String summary = aiService.generateSummary(contentBuilder.toString());

        // 保存会议纪要
        MeetingSummary meetingSummary = new MeetingSummary();
        meetingSummary.setMeetingId(dto.getMeetingId());
        meetingSummary.setContent(summary);
        meetingSummary.setMessageType(dto.getMessageType());
        meetingSummary.setStatus("DRAFT");
        meetingSummary.setCreateTime(LocalDateTime.now());
        meetingSummary.setUpdateTime(LocalDateTime.now());
        meetingSummary.setCreatorId(dto.getCreatorId());

        this.save(meetingSummary);
        return meetingSummary;
    }

    @Override
    public MeetingSummary updateSummary(SummaryUpdateDTO dto) {
        MeetingSummary summary = this.getById(dto.getSummaryId());
        if (summary == null) {
            throw new RuntimeException("会议纪要不存在");
        }

        summary.setContent(dto.getContent());
        summary.setStatus("REVIEWED");
        summary.setUpdateTime(LocalDateTime.now());
        summary.setReviewerId(dto.getReviewerId());

        this.updateById(summary);
        return summary;
    }

    @Override
    public MeetingSummary getSummaryByMeetingId(Long meetingId) {
        return this.lambdaQuery()
                .eq(MeetingSummary::getMeetingId, meetingId)
                .orderByDesc(MeetingSummary::getCreateTime)
                .last("LIMIT 1")
                .one();
    }
}