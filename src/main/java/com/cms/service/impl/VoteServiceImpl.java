package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.common.Result;
import com.cms.dto.VoteCreateDTO;
import com.cms.mapper.VoteMapper;
import com.cms.mapper.VoteOptionMapper;
import com.cms.mapper.VoteRecordMapper;
import com.cms.pojo.Vote;
import com.cms.pojo.VoteOption;
import com.cms.pojo.VoteRecord;
import com.cms.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VoteServiceImpl implements VoteService {

    private final VoteMapper voteMapper;
    private final VoteOptionMapper optionMapper;
    private final VoteRecordMapper recordMapper;

    @Override
    @Transactional
    public Result<?> createVote(VoteCreateDTO voteDTO) {
        Vote vote = new Vote();
        vote.setMeetingId(voteDTO.getMeetingId());
        vote.setTitle(voteDTO.getTitle());
        vote.setDescription(voteDTO.getDescription());
        vote.setIsMultiple(voteDTO.getIsMultiple());
        vote.setIsAnonymous(voteDTO.getIsAnonymous());
        vote.setStartTime(new Date());
        vote.setEndTime(voteDTO.getEndTime());
        vote.setStatus("进行中");

        voteMapper.insert(vote);

        // 创建投票选项
        for (String optionContent : voteDTO.getOptions()) {
            VoteOption option = new VoteOption();
            option.setVoteId(vote.getVoteId());
            option.setContent(optionContent);
            option.setVoteCount(0);
            optionMapper.insert(option);
        }

        return Result.success(vote);
    }

    @Override
    public Result<?> getMeetingVotes(Long meetingId) {
        LambdaQueryWrapper<Vote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vote::getMeetingId, meetingId)
                .orderByDesc(Vote::getCreatedAt);

        List<Vote> votes = voteMapper.selectList(wrapper);
        return Result.success(votes);
    }

    @Override
    public Result<?> getVoteDetail(Long voteId) {
        Vote vote = voteMapper.selectById(voteId);
        if (vote == null) {
            return Result.error("投票不存在");
        }

        LambdaQueryWrapper<VoteOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoteOption::getVoteId, voteId);
        List<VoteOption> options = optionMapper.selectList(wrapper);

        // 可以在这里添加更多详细信息，如投票统计等

        return Result.success(options);
    }

    @Override
    @Transactional
    public Result<?> submitVote(Long voteId, List<Long> optionIds, Long userId) {
        Vote vote = voteMapper.selectById(voteId);
        if (vote == null) {
            return Result.error("投票不存在");
        }

        if (vote.getEndTime().before(new Date())) {
            return Result.error("投票已结束");
        }

        // 检查是否已投票
        LambdaQueryWrapper<VoteRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(VoteRecord::getVoteId, voteId)
                .eq(VoteRecord::getUserId, userId);
        if (recordMapper.selectCount(recordWrapper) > 0) {
            return Result.error("您已参与过此投票");
        }

        // 单选投票检查
        if (!vote.getIsMultiple() && optionIds.size() > 1) {
            return Result.error("此投票为单选");
        }

        // 记录投票
        for (Long optionId : optionIds) {
            VoteRecord record = new VoteRecord();
            record.setVoteId(voteId);
            record.setOptionId(optionId);
            record.setUserId(userId);
            recordMapper.insert(record);

            // 更新选项票数
            VoteOption option = optionMapper.selectById(optionId);
            option.setVoteCount(option.getVoteCount() + 1);
            optionMapper.updateById(option);
        }

        return Result.success("投票成功");
    }
    @Override
    public Result<?> getVoteParticipantsCount(Long voteId) {
        // 检查投票是否存在
        Vote vote = voteMapper.selectById(voteId);
        if (vote == null) {
            return Result.error("投票不存在");
        }

        // 统计参与人数（去重）
        Integer count = voteMapper.countDistinctParticipants(voteId);

        Map<String, Object> data = new HashMap<>();
        data.put("voteId", voteId);
        data.put("participantsCount", count);

        return Result.success(data);
    }
}