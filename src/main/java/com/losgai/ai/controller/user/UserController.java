package com.losgai.ai.controller.user;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.losgai.ai.common.Result;
import com.losgai.ai.dto.LoginDto;
import com.losgai.ai.entity.user.User;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.service.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/user")
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping("/auth/doLogin")
    @Tag(name = "用户登录",description = "用户单点登录")
    public Result<SaTokenInfo> doLogin(@RequestBody LoginDto loginDto) {
        ResultCodeEnum resultCodeEnum = userService.doLogin(loginDto);
        if(Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())){
            // 获取令牌
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
            // 返回给前端
            return Result.success(tokenInfo);
        }
        return Result.error(resultCodeEnum.getMessage());
    }

    @PostMapping("/auth/doRegister")
    @Tag(name = "用户注册",description = "用户单点注册")
    public Result<SaTokenInfo> doRegister(@RequestBody User user) {
        ResultCodeEnum resultCodeEnum = userService.doRegister(user);
        if(Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())){
            // 获取令牌
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
            // 返回给前端
            return Result.success(tokenInfo);
        }
        return Result.error(resultCodeEnum.getMessage());
    }

    @RequestMapping("/doLogout")
    @Tag(name = "用户注销", description = "单端独立注销")
    public Result<String> logoutByAlone() {
        StpUtil.logout();
        return Result.success("注销成功");
    }

}
