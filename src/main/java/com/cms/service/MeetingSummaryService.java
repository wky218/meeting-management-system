package com.cms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cms.dto.SummaryGenerateDTO;
import com.cms.dto.SummaryUpdateDTO;
import com.cms.pojo.MeetingSummary;

public interface MeetingSummaryService extends IService<MeetingSummary> {
    /**
     * 生成会议纪要
     */
    MeetingSummary generateSummary(SummaryGenerateDTO dto);

    /**
     * 更新会议纪要
     */
    MeetingSummary updateSummary(SummaryUpdateDTO dto);

    /**
     * 根据会议ID获取最新的会议纪要
     */
    MeetingSummary getSummaryByMeetingId(Long meetingId);
}