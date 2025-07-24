package com.losgai.ai.service.user.impl;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.losgai.ai.dto.LoginDto;
import com.losgai.ai.entity.user.User;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.mapper.UserMapper;
import com.losgai.ai.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    @Description("执行登录的方法")
    public ResultCodeEnum doLogin(LoginDto loginDto) {
        // 1. 根据用户名查询用户
        String username = loginDto.getUserName();
        if (StrUtil.isBlank(username)) {
            return ResultCodeEnum.LOGIN_ERROR;
        }
        User user = userMapper.selectByUsername(loginDto.getUserName());
        if (user == null) {
            return ResultCodeEnum.LOGIN_ERROR;
        }

        // 2.加密密码
        String encryptedPwd = SaSecureUtil.sha256(loginDto.getPassword());
        // 3.验证密码
        if (user.getPassword().equals(encryptedPwd)) {
            // 第1步，先登录上
            StpUtil.login(user.getId(), loginDto.getRememberMe());
            return ResultCodeEnum.SUCCESS;
        }
        // TODO 目前验证码直接在前端验证，后续再实现

        return ResultCodeEnum.LOGIN_ERROR;
    }

    @Override
    @Description("执行注册的方法")
    public ResultCodeEnum doRegister(User user) {
        // 1.判断对应用户是否存在，防止重复注册
        String email = user.getEmail();
        String userPhone = user.getPhone();
        if (userMapper.existsByUsername(email, userPhone) >= 1) {
            return ResultCodeEnum.USER_NAME_IS_EXISTS;
        }
        // 2.插入用户数据
        user.setStatus(1);
        user.setRole(0L);
        user.setDeleted(0);
        user.setPassword(SaSecureUtil.sha256(user.getPassword()));
        user.setCreateTime(Date.from(Instant.now()));
        user.setUpdateTime(Date.from(Instant.now()));
        userMapper.insert(user);
        // 第1步，先登录上
        StpUtil.login(user.getId(), false);
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    public User getUserInfo() {
        return userMapper.selectByPrimaryKey(StpUtil.getLoginIdAsLong());
    }
}
