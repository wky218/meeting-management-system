package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.pojo.BlackList;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface BlacklistMapper extends BaseMapper<BlackList> {
    @Select("SELECT u.user_id, u.username, mp.role " +
            "FROM meeting_participants mp " +
            "INNER JOIN users u ON mp.user_id = u.user_id " +
            "WHERE mp.meeting_id = #{meetingId} " +
            "AND mp.role IN ('HOST', 'ADMIN')")
    List<Map<String, Object>> selectMeetingManagers(Long meetingId);
} 