package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.pojo.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select("SELECT * FROM tags WHERE tag_name = #{tagName}")
    Tag findByTagName(String tagName);

    @Insert("INSERT INTO tags (tag_name) VALUES (#{tagName})")
    int createTag(String tagName);
}