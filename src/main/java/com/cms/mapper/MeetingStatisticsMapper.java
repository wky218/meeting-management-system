package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.pojo.MeetingStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
@Mapper
@Repository
public interface MeetingStatisticsMapper extends BaseMapper<MeetingStatistics> {

    @Select("SELECT * FROM statistics WHERE meeting_id = #{meetingId}")
    MeetingStatistics getByMeetingId(Long meetingId);

    @Update("UPDATE statistics SET " +
            "total_participants = #{totalParticipants}, " +
            "signed_in = #{signedIn}, " +
            "leave_count = #{leaveCount} " +
            "WHERE meeting_id = #{meetingId}")
    int updateStatistics(MeetingStatistics statistics);

    @Select("SELECT COUNT(*) FROM statistics WHERE meeting_id = #{meetingId}")
    int existsByMeetingId(Long meetingId);

    @Delete("DELETE FROM statistics WHERE meeting_id = #{meetingId}")
    int deleteByMeetingId(@Param("meetingId") Long meetingId);
}