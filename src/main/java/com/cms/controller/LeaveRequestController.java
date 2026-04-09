package com.cms.controller;

import com.cms.common.Result;
import com.cms.dto.LeaveRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.cms.service.LeaveRequestService;
@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping("/submit")
    public Result<?> submitLeaveRequest(@RequestBody LeaveRequestDTO request) {
        return leaveRequestService.submitLeaveRequest(request);
    }

    @PostMapping("/approve/{leaveId}")
    public Result<?> approveLeaveRequest(@PathVariable Long leaveId) {
        return leaveRequestService.approveLeaveRequest(leaveId);
    }

    @PostMapping("/reject/{leaveId}")
    public Result<?> rejectLeaveRequest(@PathVariable Long leaveId) {
        return leaveRequestService.rejectLeaveRequest(leaveId);
    }

    @GetMapping("/list/{meetingId}")
    public Result<?> getLeaveRequestsByMeetingId(@PathVariable Long meetingId) {
        return leaveRequestService.getLeaveRequestsByMeetingId(meetingId);
    }
}