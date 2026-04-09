package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("meeting_tag_relations")
public class MeetingTagRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long meetingId;
    private Long tagId;
}