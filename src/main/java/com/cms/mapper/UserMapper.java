package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 根据用户名查询用户
    User selectByUsername(@Param("username") String username);

    // 用户登录
    User login(@Param("username") String username, @Param("password") String password);

    // 查询用户列表
    List<User> selectUserList(@Param("username") String username, @Param("departmentId") Integer departmentId);

    // 检查用户名是否存在
    int checkUsername(@Param("username") String username);

    // 根据账号获取用户信息
    User getUserInfo(@Param("account") String account);
    @Select("SELECT username FROM users WHERE user_id = #{userId}")
    String getUsernameById(@Param("userId") Long userId);
}