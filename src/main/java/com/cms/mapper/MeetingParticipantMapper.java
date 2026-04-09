package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.pojo.MeetingParticipant;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MeetingParticipantMapper extends BaseMapper<MeetingParticipant> {
    @Delete("DELETE FROM meeting_participants WHERE meeting_id = #{meetingId}")
    int deleteByMeetingId(Long meetingId);
    @Select("SELECT participant_id FROM meeting_participants WHERE meeting_id = #{meetingId}")
    List<Long> selectParticipantIdsByMeetingId(Long meetingId);
    @Select("SELECT meeting_id FROM meeting_participants WHERE participant_id = #{participantId}")
    List<Long> selectMeetingIdsByParticipantId(Long participantId);
}