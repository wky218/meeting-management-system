package com.cms.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.common.Result;
import com.cms.dto.LeaveRequestDTO;
import com.cms.mapper.LeaveRequestMapper;
import com.cms.mapper.MeetingMapper;
import com.cms.mapper.MeetingParticipantMapper;
import com.cms.mapper.UserMapper;
import com.cms.pojo.LeaveRequest;
import com.cms.pojo.Meeting;
import com.cms.pojo.MeetingParticipant;
import com.cms.pojo.User;
import com.cms.service.LeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestMapper leaveRequestMapper;
    private final MeetingMapper meetingMapper;
    private final UserMapper userMapper;
    private final MeetingParticipantMapper participantMapper;

    @Override
    @Transactional
    public Result<?> submitLeaveRequest(LeaveRequestDTO request) {
        // 验证会议是否存在
        Meeting meeting = meetingMapper.selectById(request.getMeetingId());
        if (meeting == null) {
            return Result.error("会议不存在");
        }
        // 验证用户是否存在
        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 验证用户是否是会议参与者
        LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingParticipant::getMeetingId, request.getMeetingId())
                .eq(MeetingParticipant::getUserId, request.getUserId());
        MeetingParticipant participant = participantMapper.selectOne(wrapper);
        if (participant == null) {
            return Result.error("您不是该会议的参与者");
        }
        // 创建请假申请
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setUserId(request.getUserId());
        leaveRequest.setMeetingId(request.getMeetingId());
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus("待审批");
        // 更新参会人员状态
        participant.setLeaveStatus("请假审核中");
        participantMapper.updateById(participant);

        if (leaveRequestMapper.insert(leaveRequest) > 0) {
            return Result.success("请假申请提交成功");
        }
        return Result.error("请假申请提交失败");
    }

    @Override
    @Transactional
    public Result<?> approveLeaveRequest(Long leaveId) {
        LeaveRequest request = leaveRequestMapper.selectById(leaveId);
        if (request == null) {
            return Result.error("请假申请不存在");
        }

        request.setStatus("通过");
        if (leaveRequestMapper.updateById(request) > 0) {
            // 更新参会人员状态为请假
            LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MeetingParticipant::getMeetingId, request.getMeetingId())
                    .eq(MeetingParticipant::getUserId, request.getUserId());

            MeetingParticipant participant = participantMapper.selectOne(wrapper);
            if (participant != null) {
                participant.setLeaveStatus("请假");
                participantMapper.updateById(participant);
            }

            return Result.success("请假申请已通过");
        }
        return Result.error("审批失败");
    }

    @Override
    @Transactional
    public Result<?> rejectLeaveRequest(Long leaveId) {
        LeaveRequest request = leaveRequestMapper.selectById(leaveId);
        if (request == null) {
            return Result.error("请假申请不存在");
        }

        request.setStatus("拒绝");
        if (leaveRequestMapper.updateById(request) > 0) {
            // 更新参会人员状态为请假驳回
            LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MeetingParticipant::getMeetingId, request.getMeetingId())
                    .eq(MeetingParticipant::getUserId, request.getUserId());

            MeetingParticipant participant = participantMapper.selectOne(wrapper);
            if (participant != null) {
                participant.setLeaveStatus("请假驳回");
                participantMapper.updateById(participant);
            }

            return Result.success("请假申请已拒绝");
        }
        return Result.error("审批失败");
    }

    @Override
    public Result<?> getLeaveRequestsByMeetingId(Long meetingId) {
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaveRequest::getMeetingId, meetingId)
                .orderByDesc(LeaveRequest::getCreatedAt);

        List<LeaveRequest> requests = leaveRequestMapper.selectList(wrapper);
        return Result.success(requests);
    }
}