package com.cms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cms.common.Result;
import com.cms.dto.LoginDTO;
import com.cms.dto.RegisterDTO;
import com.cms.pojo.User;

public interface UserService extends IService<User> {
    User getUserByUsername(String username);
   //登录注册
    User login(LoginDTO loginDTO) throws Exception;
    void register(RegisterDTO registerDTO);
    User getUserInfo(String account);
    Result<?> updatePassword(Long userId, String oldPassword, String newPassword);

    Result<?> deleteAccount(Long userId, String password);


    Result<?> searchUsers(Long userId, String username, String email, String phone);

    Result<?> updateUserInfo(Long userId, String username, String email, String phone);
}