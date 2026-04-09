package com.cms.pojo;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("vote_records")
public class VoteRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long recordId;
    private Long voteId;
    private Long optionId;
    private Long userId;
    @TableField("created_at")
    private Date createdAt;
}