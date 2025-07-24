package com.losgai.ai.service.user;

import com.losgai.ai.dto.LoginDto;
import com.losgai.ai.entity.user.User;
import com.losgai.ai.enums.ResultCodeEnum;

public interface UserService {

    ResultCodeEnum doLogin(LoginDto loginDto);

    ResultCodeEnum doRegister(User user);

    User getUserInfo();
}
