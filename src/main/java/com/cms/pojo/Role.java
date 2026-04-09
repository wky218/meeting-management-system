package com.cms.pojo;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
@Data
@TableName("roles")
public class Role {
    @TableId(type = IdType.ASSIGN_ID)
    private Long roleId;
    private String roleName;
    private String permissions;
}