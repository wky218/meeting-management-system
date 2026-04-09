package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_tag_relations")
public class UserTagRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long tagId;
}