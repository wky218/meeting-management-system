package com.cms.controller;

import com.cms.common.Result;
import com.cms.dto.VoteCreateDTO;
import com.cms.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping("/create")
    public Result<?> createVote(@RequestBody VoteCreateDTO voteDTO) {
        return voteService.createVote(voteDTO);
    }

    @GetMapping("/meeting/{meetingId}")
    public Result<?> getMeetingVotes(@PathVariable Long meetingId) {
        return voteService.getMeetingVotes(meetingId);
    }

    @GetMapping("/detail/{voteId}")
    public Result<?> getVoteDetail(@PathVariable Long voteId) {
        return voteService.getVoteDetail(voteId);
    }
    //提交投票
    @PostMapping("/submit/{voteId}")
    public Result<?> submitVote(
            @PathVariable Long voteId,
            @RequestParam Long userId,
            @RequestBody List<Long> optionIds) {
        return voteService.submitVote(voteId, optionIds, userId);
    }
    //投票参与人数统计
    @GetMapping("/participants-count/{voteId}")
    public Result<?> getParticipantsCount(@PathVariable Long voteId) {
        return voteService.getVoteParticipantsCount(voteId);
    }
}