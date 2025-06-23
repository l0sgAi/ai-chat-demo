package com.losgai.ai.service.user;

import com.losgai.ai.dto.LoginDto;
import com.losgai.ai.entity.user.User;
import com.losgai.ai.enums.ResultCodeEnum;

import java.util.List;

public interface UserService {

    ResultCodeEnum doLogin(LoginDto loginDto);

    ResultCodeEnum doRegister(User user);

    ResultCodeEnum addStudent(User user);

    ResultCodeEnum updateStudent(User user);

    ResultCodeEnum delete(Long id);

    List<User> queryByKeyWord(String keyWord);
}
