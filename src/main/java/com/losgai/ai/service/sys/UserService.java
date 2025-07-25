package com.losgai.ai.service.sys;

import com.losgai.ai.dto.LoginDto;
import com.losgai.ai.entity.sys.User;
import com.losgai.ai.enums.ResultCodeEnum;

public interface UserService {

    ResultCodeEnum doLogin(LoginDto loginDto);

    ResultCodeEnum doRegister(User user);

    User getUserInfo();
}
