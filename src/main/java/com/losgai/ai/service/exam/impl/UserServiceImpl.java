package com.losgai.ai.service.exam.impl;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.losgai.ai.dto.ChangePwdDto;
import com.losgai.ai.dto.LoginDto;
import com.losgai.ai.entity.exam.User;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.mapper.UserMapper;
import com.losgai.ai.service.exam.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;

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
            // 保存用户完整信息到 Session 中
            StpUtil.getSession().set("user", user);
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
        // 保存用户完整信息到 Session 中
        StpUtil.getSession().set("user", user);
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("新增学生信息")
    public ResultCodeEnum addStudent(User user) {
        // 1.判断对应用户是否存在，防止重复注册
        String email = user.getEmail(); // 对于学生来说是学号
        String userPhone = user.getPhone();
        if (userMapper.existsByUsername(email, userPhone) >= 1) {
            return ResultCodeEnum.USER_NAME_IS_EXISTS;
        }
        // 2.插入用户数据
        user.setStatus(1);
        user.setRole(0L);
        user.setDeleted(0);
        // 默认密码，学号+姓名
        user.setPassword(SaSecureUtil.sha256(email + user.getUsername()));
        user.setCreateTime(Date.from(Instant.now()));
        user.setUpdateTime(Date.from(Instant.now()));
        userMapper.insert(user);
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("更新学生信息")
    public ResultCodeEnum updateStudent(User user) {
        // 密码加密储存
        user.setPassword(SaSecureUtil.sha256(user.getPassword()));
        // 更新更新时间
        user.setUpdateTime(Date.from(Instant.now()));
        int i = userMapper.updateByPrimaryKeySelective(user);
        if (i < 1) {
            return ResultCodeEnum.DATA_ERROR;
        }
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("删除学生信息")
    public ResultCodeEnum delete(Long id) {
        int i = userMapper.deleteByPrimaryKey(id);
        if (i < 1) {
            return ResultCodeEnum.DATA_ERROR;
        }
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("查询学生信息")
    public List<User> queryByKeyWord(String keyWord) {
        return userMapper.queryByKeyWord(keyWord);
    }

    @Override
    public ResultCodeEnum changePwd(ChangePwdDto changePwdDto) {
        if (StpUtil.isLogin()) { // 判断是否登录
            // 从Session中获取用 户信息（如果登录时已保存）
            User userCur = ((JSONObject) StpUtil.getSession().get("user")).to(User.class);
            String old = SaSecureUtil.sha256(changePwdDto.getOldPassword());
            String newPwd = SaSecureUtil.sha256(changePwdDto.getNewPassword());
            // 校验旧密码
            if (userCur.getPassword().equals(old)) {
                // 更新密码
                userMapper.updatePwd(newPwd, userCur.getId());
                // 保存用户完整信息到 Session 中
                userCur.setPassword(newPwd);
                StpUtil.getSession().set("user", userCur);
                return ResultCodeEnum.SUCCESS;
            }
        }
        return ResultCodeEnum.LOGIN_ERROR;
    }
}
