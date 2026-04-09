package com.cms.dto;

import lombok.Data;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class VoteCreateDTO {
    private Long meetingId;
    private String title;
    private String description;
    private Boolean isMultiple;  //多选
    private Boolean isAnonymous;  //匿名

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    private List<String> options;  // 投票选项列表
}
