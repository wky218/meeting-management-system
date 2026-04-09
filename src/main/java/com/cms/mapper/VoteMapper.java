package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.pojo.Vote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VoteMapper extends BaseMapper<Vote> {

    @Select("SELECT COUNT(DISTINCT user_id) FROM vote_records WHERE vote_id = #{voteId}")
    Integer countDistinctParticipants(Long voteId);
}