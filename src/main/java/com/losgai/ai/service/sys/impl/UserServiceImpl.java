package com.losgai.ai.service.sys.impl;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.losgai.ai.dto.LoginDto;
import com.losgai.ai.entity.sys.User;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.mapper.UserMapper;
import com.losgai.ai.service.sys.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    /**
     * 工作流程：
     * 1. Spring AOP在方法执行前，会根据指定的key (即用户ID) 去名为 "userInfo" 的Redis缓存中查找数据。
     * 2. 如果找到，则直接返回缓存中的User对象，方法体内的代码将不会被执行。
     * 3. 如果未找到，则执行方法体内的代码 `userMapper.selectByPrimaryKey(...)` 从数据库查询。
     * 4. 方法成功返回后，Spring Cache会将返回的User对象存入Redis缓存，key为用户ID，并设置3小时的过期时间。
     */
    @Override
    @Cacheable(cacheNames = "userInfo", key = "T(cn.dev33.satoken.stp.StpUtil).getLoginIdAsLong()")
    public User getUserInfo() {
        log.info("缓存未命中，从数据库查询用户信息..."); // 添加日志用于测试
        return userMapper.selectByPrimaryKey(StpUtil.getLoginIdAsLong());
    }

    /**
     * 更新用户信息的方法，并在更新后清除缓存
     *
     * - cacheNames = "userInfo": 指定要操作的缓存空间。
     * - key = "#user.id": 使用SpEL指定要清除的缓存key。#user表示方法参数user对象，.id获取其id属性。
     *
     * 当用户信息（如密码、昵称）被修改时，必须清除旧的缓存，否则用户将看到过时的数据。
     */
    @CacheEvict(cacheNames = "userInfo", key = "#user.id")
    public void updateUserInfo(User user) {
        // 1. 更新数据库
        user.setUpdateTime(Date.from(Instant.now()));
        userMapper.updateByPrimaryKeySelective(user);
        // 2. 方法执行成功后，Spring会自动清除ID对应的缓存
        log.info("用户信息已更新，清除缓存: {}", user.getId());
    }
}
