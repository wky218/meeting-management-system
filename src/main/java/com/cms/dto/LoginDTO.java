package com.cms.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String username;
    private String password;
}
//DTO（数据传输对象）：
//- 主要用于服务层之间的数据传输
//- 通常用于接收前端请求的数据