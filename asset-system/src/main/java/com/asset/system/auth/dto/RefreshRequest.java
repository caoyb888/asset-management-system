package com.asset.system.auth.dto;

import lombok.Data;

/**
 * Token 刷新请求 DTO
 */
@Data
public class RefreshRequest {

    /** Refresh Token */
    private String refreshToken;
}
