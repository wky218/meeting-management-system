package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.pojo.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    @Select("SELECT * FROM chat_messages WHERE meeting_id = #{meetingId} AND message_type = #{messageType} ORDER BY send_time ASC")
    List<ChatMessage> getMeetingMessages(@Param("meetingId") Long meetingId, @Param("messageType") String messageType);
}