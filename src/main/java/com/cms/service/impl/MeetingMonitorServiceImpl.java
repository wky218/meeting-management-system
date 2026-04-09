package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.common.Result;
import com.cms.mapper.MeetingParticipantMapper;
import com.cms.pojo.MeetingParticipant;
import com.cms.service.MeetingMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingMonitorServiceImpl implements MeetingMonitorService {

    private final MeetingParticipantMapper participantMapper;
    private static final long INACTIVE_THRESHOLD = 5; // 5分钟不活跃判定为离线

    // 修改为无参数方法
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void checkInactiveParticipants() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(INACTIVE_THRESHOLD);
        Date thresholdDate = Date.from(threshold.atZone(ZoneId.systemDefault()).toInstant());

        LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(MeetingParticipant::getLastActiveTime, thresholdDate);

        List<MeetingParticipant> inactiveParticipants = participantMapper.selectList(wrapper);
        for (MeetingParticipant participant : inactiveParticipants) {
            participant.setLastActiveTime(null);  // 清除最后活跃时间，表示离线
            participantMapper.updateById(participant);
        }
    }

    @Override
    public Result<?> updateParticipantStatus(Long meetingId, Long userId) {
        LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .eq(MeetingParticipant::getUserId, userId);

        MeetingParticipant participant = participantMapper.selectOne(wrapper);
        if (participant != null) {
            participant.setLastActiveTime(new Date());
            participantMapper.updateById(participant);
            return Result.success("更新活跃状态成功");
        }
        return Result.success("用户不在会议中");
    }

    @Override
    public Result<?> getOnlineParticipants(Long meetingId) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(INACTIVE_THRESHOLD);
        Date thresholdDate = Date.from(threshold.atZone(ZoneId.systemDefault()).toInstant());

        LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .ge(MeetingParticipant::getLastActiveTime, thresholdDate);

        List<MeetingParticipant> onlineParticipants = participantMapper.selectList(wrapper);
        return Result.success(onlineParticipants);
    }
}
