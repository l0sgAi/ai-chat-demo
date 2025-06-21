package com.losgai.ai.entity.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户信息表
 * @TableName user
 */
@Data
public class User implements Serializable {

    /**
     * 用户ID（更新时校验，创建时一般由数据库自动生成）
     */
    private Long id;

    /**
     * 用户名（必填，长度限制）
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度应在3到20个字符之间")
    private String username;

    /**
     * 加密后的密码（必填，建议长度限制）
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度应在6到100个字符之间")
    private String password;

    /**
     * 邮箱（可选，但如果填了必须是邮箱格式）
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号（可选，但如果填了需要符合格式）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 昵称（可选，但限制长度）
     */
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    private String nickname;

    /**
     * 头像链接（可选）
     */
    @Size(max = 255, message = "头像链接长度不能超过255个字符")
    private String avatarUrl;

    /**
     * 性别：0=未知，1=男，2=女
     */
    @Min(value = 0, message = "性别最小为0")
    @Max(value = 2, message = "性别最大为2")
    private Integer gender;

    /**
     * 出生日期（可选，但不能为未来时间）
     */
    @Past(message = "出生日期必须是过去的时间")
    private Date birthdate;

    /**
     * 状态：0=禁用，1=启用
     */
//    @NotNull(message = "用户状态不能为空")
    @Min(value = 0, message = "状态最小为0")
    @Max(value = 1, message = "状态最大为1")
    private Integer status;

    /**
     * 角色id（必填）
     */
//    @NotNull(message = "角色ID不能为空")
    private Long role;

    /**
     * 创建时间（后端生成，一般不需前端传递）
     */
    private Date createTime;

    /**
     * 更新时间（后端生成）
     */
    private Date updateTime;

    /**
     * 逻辑删除：0=正常，1=已删除
     */
    @Min(value = 0, message = "删除状态最小为0")
    @Max(value = 1, message = "删除状态最大为1")
    private Integer deleted;
}
