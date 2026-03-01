package com.asset.system.user.dto;

import lombok.Data;

/** 修改个人资料 DTO */
@Data
public class UserProfileDTO {
    private String realName;
    private String phone;
    private String email;
    private String avatar;
}
