package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.common.Result;
import com.cms.enums.MeetingVisibility;
import com.cms.mapper.MeetingMapper;
import com.cms.mapper.MeetingParticipantMapper;
import com.cms.mapper.MeetingStatisticsMapper;
import com.cms.pojo.Meeting;
import com.cms.pojo.MeetingParticipant;
import com.cms.pojo.MeetingStatistics;
import com.cms.service.SignInService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignInServiceImpl implements SignInService {

    private final MeetingParticipantMapper participantMapper;
    private final MeetingStatisticsMapper statisticsMapper;
    private final MeetingMapper meetingMapper;
    @Override
    @Transactional
    public Result<?> signIn(Long meetingId, Long userId) {
        // 先检查会议类型
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return Result.error("会议不存在");
        }
        // 如果是公开会议，不需要签到
        if (MeetingVisibility.PUBLIC.equals(meeting.getVisibility())) {
            return Result.error("公开会议无需签到");
        }
        // 更新参会者签到状态
        LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .eq(MeetingParticipant::getUserId, userId);

        MeetingParticipant participant = participantMapper.selectOne(wrapper);
        if (participant == null) {
            return Result.error("您不是该会议的参与者");
        }

        // 检查是否已经签到
        if ("已签到".equals(participant.getSignInStatus())) {
            return Result.error("您已经签到过了");
        }
        // 设置签到状态和时间
        participant.setSignInStatus("已签到");
        participant.setSignInTime(new Date());
        participant.setLastActiveTime(new Date());
        participantMapper.updateById(participant);
        // 更新统计信息
        updateStatistics(meetingId);

        return Result.success("签到成功");
    }
    @Override
    public Result<?> getSignInStatus(Long meetingId, Long userId) {
        // 先检查会议类型
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return Result.error("会议不存在");
        }

        // 如果是公开会议，直接返回无需签到
        if (MeetingVisibility.PUBLIC.equals(meeting.getVisibility())) {
            return Result.success("公开会议无需签到");
        }

        LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .eq(MeetingParticipant::getUserId, userId);

        MeetingParticipant participant = participantMapper.selectOne(wrapper);
        return Result.success(participant != null ? participant.getSignInStatus() : "未参会");
    }

    @Override
    public Result<?> getMeetingStatistics(Long meetingId) {
        // 先检查会议类型
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return Result.error("会议不存在");
        }

        // 如果是公开会议，返回无需统计
        if (MeetingVisibility.PUBLIC.equals(meeting.getVisibility())) {
            return Result.success("公开会议无需统计签到信息");
        }
        // 先查询是否存在统计记录
        MeetingStatistics statistics = statisticsMapper.getByMeetingId(meetingId);

        if (statistics == null) {
            // 如果不存在，创建新的统计记录
            LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MeetingParticipant::getMeetingId, meetingId);

            List<MeetingParticipant> participants = participantMapper.selectList(wrapper);
            int totalParticipants = participants.size();
            int signedIn = (int) participants.stream()
                    .filter(p -> "已签到".equals(p.getSignInStatus()))
                    .count();
            int leaveCount = (int) participants.stream()
                    .filter(p -> "请假".equals(p.getLeaveStatus()))
                    .count();

            statistics = new MeetingStatistics();
            statistics.setMeetingId(meetingId);
            statistics.setTotalParticipants(totalParticipants);
            statistics.setSignedIn(signedIn);
            statistics.setLeaveCount(leaveCount);

            statisticsMapper.insert(statistics);
        }

        return Result.success(statistics);
    }
    private void updateStatistics(Long meetingId) {
        // 统计签到人数和请假人数
        LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingParticipant::getMeetingId, meetingId);

        List<MeetingParticipant> participants = participantMapper.selectList(wrapper);
        int totalParticipants = participants.size();
        int signedIn = (int) participants.stream()
                .filter(p -> "已签到".equals(p.getSignInStatus()))
                .count();
        int leaveCount = (int) participants.stream()
                .filter(p -> "请假".equals(p.getLeaveStatus()))
                .count();

        // 更新或插入统计数据
        MeetingStatistics statistics = new MeetingStatistics();
        statistics.setMeetingId(meetingId);
        statistics.setTotalParticipants(totalParticipants);
        statistics.setSignedIn(signedIn);
        statistics.setLeaveCount(leaveCount);

        if (statisticsMapper.existsByMeetingId(meetingId) > 0) {
            statisticsMapper.updateStatistics(statistics);
        } else {
            statisticsMapper.insert(statistics);
        }
    }
}