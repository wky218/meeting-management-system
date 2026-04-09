package com.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.common.Result;
import com.cms.dto.MeetingCreateDTO;
import com.cms.dto.MeetingQueryDTO;
import com.cms.dto.MeetingSearchDTO;
import com.cms.dto.UserMeetingParticipationDTO;
import com.cms.enums.MeetingVisibility;
import com.cms.pojo.Meeting;
import com.cms.service.MeetingService;
import com.cms.service.WebRTCService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/meeting")
@RequiredArgsConstructor
public class MeetingController {
    private final WebRTCService webRTCService;
    private final MeetingService meetingService;
    //公开会议的创建
    @PostMapping("/createPublic")
    public Result<?> createPublicMeeting(@RequestBody MeetingCreateDTO meetingDTO) {
        return meetingService.createPublicMeeting(meetingDTO);
    }
    //私有会议创建
    @PostMapping("/create")
    public Result<?> createMeeting(@RequestBody MeetingCreateDTO meetingDTO) {
        return meetingService.createMeeting(meetingDTO);
    }
    //获取会议列表
    @GetMapping("/list")
    public Result<Page<Meeting>> listMeetings(MeetingQueryDTO queryDTO) {
        return meetingService.listMeetings(queryDTO);
    }

    @GetMapping("/{id}")
    public Result<Meeting> getMeetingDetail(@PathVariable Long id) {
        return meetingService.getMeetingDetail(id);
    }

    @DeleteMapping("/{meetingId}/cancel")
    public Result<?> cancelMeeting(@PathVariable Long meetingId,@RequestParam Long userId) {
        return meetingService.cancelMeeting(meetingId,userId);
    }

    @PutMapping("/{meetingId}/start")
    public Result<?> startMeeting(@PathVariable Long meetingId) {
        return meetingService.startMeeting(meetingId);
    }

    @PostMapping("/{meetingId}/end")
    public Result<?> endMeeting(@PathVariable Long meetingId, @RequestParam Long userId) {
        return meetingService.endMeeting(meetingId, userId);
    }

    @GetMapping("/{meetingId}/participants")
    public Result<?> getMeetingParticipants(@PathVariable Long meetingId) {
        return meetingService.getMeetingParticipants(meetingId);
    }

    //模糊搜索公开会议
    @GetMapping("/searchPublic/{userId}")
    public Result<?> searchPublicMeetings(@PathVariable Long userId,MeetingSearchDTO searchDTO) {
        searchDTO.setUserId(userId);
        return meetingService.searchPublicMeetings(searchDTO);
    }
    //模糊搜索私有会议
    @GetMapping("/searchPrivate/{userId}")
    public Result<?> searchPrivateMeetings(@PathVariable Long userId, MeetingSearchDTO searchDTO) {
        searchDTO.setUserId(userId);
        return meetingService.searchPrivateMeetings(searchDTO);
    }
    @PutMapping("/update")
    public Result<?> updateMeeting(
            @RequestParam Long meetingId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) MeetingVisibility visibility ,
            @RequestParam(required = false) String meetingType
    ) {
        return meetingService.updateMeeting(meetingId, title, description, startTime, endTime, location, visibility, meetingType);
    }
    @PostMapping("/admin/add")
    public Result<?> addMeetingAdmin(
            @RequestParam Long meetingId,
            @RequestParam Long userId,
            @RequestParam Long currentUserId) {
        return meetingService.addMeetingAdmin(meetingId, userId, currentUserId);
    }

    @DeleteMapping("/admin/remove")
    public Result<?> removeMeetingAdmin(
            @RequestParam Long meetingId,
            @RequestParam Long userId,
            @RequestParam Long currentUserId) {
        return meetingService.removeMeetingAdmin(meetingId, userId, currentUserId);
    }
    @GetMapping("/{meetingId}/managers")
    public Result<?> getMeetingManagers(@PathVariable Long meetingId) {
        return meetingService.getMeetingManagers(meetingId);
    }
    @GetMapping("/user/{userId}/participation-history")
    public Result<List<UserMeetingParticipationDTO>> getUserParticipationHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        return Result.success(meetingService.getUserParticipationHistory(userId, startDate, endDate));
    }
}