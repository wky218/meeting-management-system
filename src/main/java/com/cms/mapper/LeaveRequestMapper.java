package com.cms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.pojo.LeaveRequest;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LeaveRequestMapper extends BaseMapper<LeaveRequest> {
    @Delete("DELETE FROM leave_requests WHERE meeting_id = #{meetingId}")
    int deleteByMeetingId(@Param("meetingId") Long meetingId);
}