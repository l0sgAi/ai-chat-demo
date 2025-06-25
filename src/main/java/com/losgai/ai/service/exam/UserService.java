package com.losgai.ai.service.exam;

import com.losgai.ai.dto.ChangePwdDto;
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

    /** 修改密码
     * @param changePwdDto 传入的用户信息
     * */
    ResultCodeEnum changePwd(ChangePwdDto changePwdDto);
}
