package com.asset.system.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 在线用户信息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineUserVO {

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 登录 IP */
    private String loginIp;

    /** 登录时间（ISO 字符串，便于 Redis JSON 存储） */
    private String loginTime;
}
