package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.pojo.MeetingTagRelation;
import com.cms.pojo.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import java.util.List;

@Mapper
public interface MeetingTagRelationMapper extends BaseMapper<MeetingTagRelation> {

    @Select("SELECT t.* FROM tags t " +
            "INNER JOIN user_tag_relations utr ON t.id = utr.tag_id " +
            "WHERE utr.user_id = #{userId}")
    List<Tag> findTagsByUserId(Long userId);

    @Select("SELECT t.* FROM tags t " +
            "INNER JOIN meeting_tag_relations mtr ON t.id = mtr.tag_id " +
            "WHERE mtr.meeting_id = #{meetingId}")
    List<Tag> findTagsByMeetingId(Long meetingId);

    @Delete("DELETE FROM meeting_tag_relations WHERE meeting_id = #{meetingId} AND tag_id = #{tagId}")
    int removeRelation(Long meetingId, Long tagId);
}