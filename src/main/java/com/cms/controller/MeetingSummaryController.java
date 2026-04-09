package com.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.common.Result;
import com.cms.mapper.MeetingSummaryMapper;
import com.cms.pojo.MeetingSummary;
import com.cms.service.SpeechToTextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/meeting/summary")
@Slf4j
public class MeetingSummaryController {

    @Autowired
    private SpeechToTextService speechToTextService;

    @Autowired
    private MeetingSummaryMapper summaryMapper;

    @PostMapping("/convert/{meetingId}")
    public Result<String> convertToText(@PathVariable Long meetingId) {
        try {
            String text = speechToTextService.convertToText(meetingId);
            if (text != null) {
                return Result.success(text);
            }
            return Result.error("语音转换失败");
        } catch (Exception e) {
            log.error("语音转文字失败: {}", e.getMessage(), e);
            return Result.error("系统错误");
        }
    }

    @GetMapping("/list/{meetingId}")
    public Result<List<MeetingSummary>> getSummaries(@PathVariable Long meetingId) {
        try {
            LambdaQueryWrapper<MeetingSummary> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MeetingSummary::getMeetingId, meetingId)
                    .orderByDesc(MeetingSummary::getCreateTime);
            List<MeetingSummary> summaries = summaryMapper.selectList(wrapper);
            return Result.success(summaries);
        } catch (Exception e) {
            log.error("获取会议纪要失败: {}", e.getMessage(), e);
            return Result.error("系统错误");
        }
    }
}