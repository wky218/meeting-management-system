package com.fww.pojo;

import java.util.List;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * 
 * @TableName project_member
 */
@TableName(value ="project_member")
@Data
public class ProjectMember{
    @TableId(type = IdType.AUTO)
    @JsonProperty(value = "pmId")
    private Integer pmId;
    
    @JsonProperty(value = "pmMid")
    private String pmMid;
   
    @JsonProperty(value = "pmPid")
    private Integer pmPid;
   
    @JsonProperty(value = "pmSalary")
    private Double pmSalary;

    @JsonProperty(value = "pmidList")
    @TableField(exist = false)
    private List<Integer> pmidList;

    @JsonProperty(value = "pmMidList")
    @TableField(exist = false)
    private List<String> pmMidList;

    @JsonProperty(value = "isInvert")
    @TableField(exist = false)
    private boolean isInvert;

    @JsonProperty(value = "targetPM")
    @TableField(exist = false)
    private ProjectMember targetPM;

    @JsonProperty(value = "sSalaryL")
    @TableField(exist = false)
    private Double sSalaryL;

    @JsonProperty(value = "sSalaryR")
    @TableField(exist = false)
    private Double sSalaryR;

    @JsonProperty(value = "pageSize")
    @TableField(exist = false)
    private long pageSize;

    @JsonProperty(value = "pageCur")
    @TableField(exist = false)
    private long pageCur;
}