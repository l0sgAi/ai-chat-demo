package com.losgai.ai.util;

import cn.dev33.satoken.stp.StpUtil;
import com.losgai.ai.entity.exam.User;

public class UserUtils {

    public static Long getUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    public static String getUserName() {
        if(StpUtil.isLogin()){ // 判断是否登录
            // 从Session中获取用 户信息（如果登录时已保存）
            User user = (User) StpUtil.getSession().get("user");
            return user.getUsername();
        }
        return null;
    }

    public static User getLoginUser(){
        if(StpUtil.isLogin()){ // 判断是否登录
            // 从Session中获取用 户信息（如果登录时已保存）
            return (User) StpUtil.getSession().get("user");
        }
        return null;
    }
}
