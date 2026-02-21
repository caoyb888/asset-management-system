package com.asset.base.controller;

import com.asset.base.entity.SysUser;
import com.asset.base.mapper.SysUserMapper;
import com.asset.common.model.R;
import com.asset.common.security.crypto.SmCryptoUtil;
import com.asset.common.security.jwt.JwtUtil;
import com.asset.common.security.util.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证接口：登录 / 用户信息 / 退出
 */
@Slf4j
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper sysUserMapper;

    /** 获取 SM2 公钥（前端初始化时调用，可选） */
    @Operation(summary = "获取SM2公钥")
    @GetMapping("/publicKey")
    public R<String> publicKey() {
        return R.ok(SmCryptoUtil.PUBLIC_KEY_HEX);
    }

    /** 登录 */
    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody LoginRequest req) {
        // 1. 查用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, req.getUsername())
                        .eq(SysUser::getStatus, 1));
        if (user == null) {
            return R.fail("用户名或密码错误");
        }

        // 2. SM2 解密前端密码
        String plainPassword;
        try {
            plainPassword = SmCryptoUtil.sm2Decrypt(req.getPassword());
        } catch (Exception e) {
            log.warn("SM2解密失败，尝试明文密码匹配: {}", e.getMessage());
            // 开发环境：解密失败时尝试直接比较明文（方便调试）
            plainPassword = req.getPassword();
        }

        // 3. SM3 验证
        if (!SmCryptoUtil.sm3Matches(plainPassword, user.getPassword())) {
            return R.fail("用户名或密码错误");
        }

        // 4. 生成 JWT
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");
        return R.ok(result);
    }

    /** 获取当前用户信息 */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/userInfo")
    public R<Map<String, Object>> userInfo(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return R.fail("未登录");
        }
        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        Map<String, Object> info = new HashMap<>();
        info.put("userId", loginUser.getUserId());
        info.put("username", loginUser.getUsername());
        info.put("nickname", user != null ? user.getRealName() : loginUser.getUsername());
        info.put("avatar", "");
        info.put("roles", List.of("admin"));
        info.put("permissions", List.of("*:*:*"));
        return R.ok(info);
    }

    /** 退出登录 */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout() {
        // 无状态JWT：前端清除token即可；有需要可在此加入黑名单
        return R.ok(null);
    }

    @Data
    public static class LoginRequest {
        private String username;
        /** SM2加密后的密码（前端用公钥加密）*/
        private String password;
    }
}
