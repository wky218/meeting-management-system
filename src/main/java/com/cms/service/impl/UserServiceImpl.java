package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cms.common.Result;
import com.cms.dto.LoginDTO;
import com.cms.mapper.UserMapper;
import com.cms.dto.RegisterDTO;
import com.cms.service.UserService;
import com.cms.pojo.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public User login(LoginDTO loginDTO) throws Exception {
        User user = userMapper.selectByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new Exception("用户不存在");
        }
        // 使用 MD5 加密比较密码
        String encodedPassword = DigestUtils.md5DigestAsHex(loginDTO.getPassword().getBytes());
        if (!encodedPassword.equals(user.getPassword())) {
            throw new Exception("密码错误");
        }
        return user;
    }

    @Override
    public void register(RegisterDTO registerDTO) {
        // 检查用户名是否存在
        if (userMapper.checkUsername(registerDTO.getUsername()) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        // 创建新用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(DigestUtils.md5DigestAsHex(registerDTO.getPassword().getBytes()));
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        // 保存用户
        save(user);
    }

    @Override
    public User getUserInfo(String account) {
        return userMapper.getUserInfo(account);
    }
    @Override
    public Result<?> updatePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null || !StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            return Result.error("参数不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            return Result.error("新密码长度必须在6-20位之间");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 对输入的旧密码进行加密后再比较
        String encryptedOldPassword = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        if (!user.getPassword().equals(encryptedOldPassword)) {
            return Result.error("原密码错误");
        }
        // 对新密码进行加密后再更新
        String encryptedNewPassword = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        user.setPassword(encryptedNewPassword);
        userMapper.updateById(user);

        return Result.success("密码修改成功");
    }
    @Override
    public Result<?> deleteAccount(Long userId, String password) {
        // 参数校验
        if (userId == null || !StringUtils.hasText(password)) {
            return Result.error("参数不能为空");
        }
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 验证密码
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!user.getPassword().equals(encryptedPassword)) {
            return Result.error("密码错误");
        }
        // 逻辑删除用户
        userMapper.deleteById(userId);  // MyBatis Plus 自动转换为逻辑删除
        return Result.success("账号注销成功");
    }
    @Override
    public Result<?> searchUsers(Long userId, String username, String email, String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 构建查询条件
        if (userId != null) {
            wrapper.likeRight(User::getUserId, userId);
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(User::getUsername, username);
        }
        if (StringUtils.hasText(email)) {
            wrapper.likeRight(User::getEmail, email);
        }
        if (StringUtils.hasText(phone)) {
            wrapper.likeRight(User::getPhone, phone);
        }

        // 只查询未删除的用户
        wrapper.eq(User::getDeleted, 0);

        List<User> users = userMapper.selectList(wrapper);
        return Result.success(users);
    }
    @Override
    public Result<?> updateUserInfo(Long userId, String username, String email, String phone) {
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }

        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 验证用户名是否已存在
        if (StringUtils.hasText(username) && !username.equals(user.getUsername())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, username);
            if (userMapper.selectCount(wrapper) > 0) {
                return Result.error("用户名已存在");
            }
            user.setUsername(username);
        }

        // 验证邮箱格式
        if (StringUtils.hasText(email) && !email.equals(user.getEmail())) {
            if (!email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
                return Result.error("邮箱格式不正确");
            }
            user.setEmail(email);
        }

        // 验证手机号格式
        if (StringUtils.hasText(phone) && !phone.equals(user.getPhone())) {
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                return Result.error("手机号格式不正确");
            }
            user.setPhone(phone);
        }

        // 更新用户信息
        userMapper.updateById(user);
        return Result.success("个人信息修改成功");
    }
}