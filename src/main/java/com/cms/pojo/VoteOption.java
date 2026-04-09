package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
@Data
@TableName("vote_options")
public class VoteOption {
    @TableId(type = IdType.ASSIGN_ID)
    private Long optionId;
    private Long voteId;
    @TableField("option_text")
    private String content;
    private Integer voteCount;     // 得票数
}