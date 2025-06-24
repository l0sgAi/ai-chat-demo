package com.losgai.ai.service.exam;

import com.losgai.ai.dto.LoginDto;
import com.losgai.ai.entity.exam.User;
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
