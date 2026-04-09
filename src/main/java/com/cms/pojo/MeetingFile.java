package com.cms.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("meeting_files")
public class MeetingFile {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fileId;
    private Long meetingId;
    private String fileName;
    private String fileType;
    private String filePath;
    private Long fileSize;
    private Long uploaderId;
    private Date uploadTime;
}