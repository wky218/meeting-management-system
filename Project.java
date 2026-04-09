package com.fww.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 
 * @TableName project
 */
@Data
@TableName(value ="project")
public class Project{
    @JsonProperty(value = "pId")
    @TableId(type = IdType.AUTO,value = "p_id")
    private Integer pId;

    @JsonProperty(value = "pName")
    private String pName;

    @JsonProperty(value = "pPrice")
    private Double pPrice;

    @JsonProperty(value = "pChargermid")
    private String pChargermid;

    @JsonProperty(value = "pPhase")
    private String pPhase;

    @JsonProperty(value = "pStatus")
    private String pStatus;

    @JsonProperty(value = "pStime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime pStime;

    @JsonProperty(value = "pEtime")
    // @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime pEtime;

    @JsonProperty(value = "pProfile")
    private String pProfile;

    @JsonProperty(value = "pidList")
    @TableField(exist = false)
    private List<Integer> pidList;

    @JsonProperty(value = "isInvert")
    @TableField(exist = false)
    private boolean isInvert;

    @JsonProperty(value = "targetPro")
    @TableField(exist = false)
    private Project targetPro;

    @JsonProperty(value = "targetsTime")
    @TableField(exist = false)
    private String targetsTime;

    @JsonProperty(value = "targeteTime")
    @TableField(exist = false)
    private String targeteTime;

    @JsonProperty(value = "sPriceL")
    @TableField(exist = false)
    private Double sPriceL;
    @JsonProperty(value = "sPriceR")
    @TableField(exist = false)
    private Double sPriceR;

    @JsonProperty(value = "seTimeL")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(exist = false)
    private LocalDateTime seTimeL;

    @JsonProperty(value = "seTimeR")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(exist = false)
    private LocalDateTime seTimeR;

    @JsonProperty(value = "ssTimeL")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(exist = false)
    private LocalDateTime ssTimeL;

    @JsonProperty(value = "ssTimeR")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(exist = false)
    private LocalDateTime ssTimeR;

    @JsonProperty(value = "pageSize")
    @TableField(exist = false)
    private long pageSize;

    @JsonProperty(value = "pageCur")
    @TableField(exist = false)
    private long pageCur;
}