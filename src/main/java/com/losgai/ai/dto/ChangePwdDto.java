package com.losgai.ai.dto;

import lombok.Data;

@Data
public class ChangePwdDto {

    private String oldPassword;

    private String newPassword;

    private String confirmPassword;

}
