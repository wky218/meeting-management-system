package com.cms.service;

import com.cms.common.Result;
import com.cms.dto.VoteCreateDTO;

import java.util.List;

public interface VoteService {
    Result<?> createVote(VoteCreateDTO voteDTO);
    Result<?> getMeetingVotes(Long meetingId);
    Result<?> getVoteDetail(Long voteId);
    Result<?> submitVote(Long voteId, List<Long> optionIds, Long userId);
    Result<?> getVoteParticipantsCount(Long voteId);
}