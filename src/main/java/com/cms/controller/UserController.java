package com.cms.controller;
import com.cms.common.Result;
import com.cms.dto.LoginDTO;
import com.cms.dto.RegisterDTO;
import com.cms.mapper.UserMapper;
import com.cms.pojo.User;
import com.cms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserService userService;
    @PostMapping("/login")
    public Result<User> login(@RequestBody LoginDTO loginDTO) {
        try {
            User user = userService.login(loginDTO);
            return Result.success(user);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO registerDTO) {
        try {
           System.out.println("用户注册");
            userService.register(registerDTO);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    @GetMapping("/getUserInfo/{account}")
    public User getUserInfo(@PathVariable("account") String account){
        System.out.println("看看id是多少"+userService.getUserInfo(account));
        return userService.getUserInfo(account);
    }
    //根据用户id获取用户名
    @GetMapping("/getname/{userId}")
    public Result<?> getNameById(@PathVariable Long userId) {
        String name = userMapper.getUsernameById(userId);
        if (name == null) {
            return Result.error("未找到该用户");
        }
        return Result.success(name);
    }


    @PostMapping("/updatePassword")
    public Result<?> updatePassword(@RequestBody Map<String, Object> params) {
        try {
            // 直接获取数字类型的 userId
            Object userIdObj = params.get("userId");
            Long userId;

            if (userIdObj instanceof Integer) {
                userId = ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                userId = (Long) userIdObj;
            } else if (userIdObj instanceof String) {
                // 对于字符串类型，使用 new BigDecimal 转换
                userId = new BigDecimal(userIdObj.toString()).longValue();
            } else {
                return Result.error("用户ID格式错误");
            }

            String oldPassword = (String) params.get("oldPassword");
            String newPassword = (String) params.get("newPassword");

            if (oldPassword == null || newPassword == null) {
                return Result.error("密码不能为空");
            }

            return userService.updatePassword(userId, oldPassword, newPassword);
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return Result.error("修改密码失败：" + e.getMessage());
        }
    }
    @DeleteMapping("/deleteAccount")
    public Result<?> deleteAccount(@RequestParam Long userId, @RequestParam String password) {
        return userService.deleteAccount(userId, password);
    }
    @GetMapping("/search")
    public Result<?> searchUsers(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        return userService.searchUsers(userId,username, email, phone);
    }
    @PutMapping("/updateInfo")
    public Result<?> updateUserInfo(
            @RequestParam Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        return userService.updateUserInfo(userId, username, email, phone);
    }
}
