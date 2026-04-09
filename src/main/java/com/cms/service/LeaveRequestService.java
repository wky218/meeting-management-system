package com.cms.service;
import com.cms.common.Result;
import com.cms.dto.LeaveRequestDTO;
public interface LeaveRequestService {
    Result<?> submitLeaveRequest(LeaveRequestDTO request);
    Result<?> approveLeaveRequest(Long leaveId);
    Result<?> rejectLeaveRequest(Long leaveId);
    Result<?> getLeaveRequestsByMeetingId(Long meetingId);
}