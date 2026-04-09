package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.pojo.Tag;
import com.cms.pojo.UserTagRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import java.util.List;

@Mapper
public interface UserTagRelationMapper extends BaseMapper<UserTagRelation> {

    @Select("SELECT t.* FROM tags t " +
            "JOIN user_tag_relations utr ON t.id = utr.tag_id " +
            "WHERE utr.user_id = #{userId}")
    List<Tag> findTagsByUserId(Long userId);

    @Delete("DELETE FROM user_tag_relations WHERE user_id = #{userId} AND tag_id = #{tagId}")
    int removeRelation(Long userId, Long tagId);
}