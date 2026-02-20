package com.asset.common.security.config;

import com.asset.common.security.utils.SmCryptoUtil;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * SM3国密密码编码器 (替代bcrypt，满足信创要求)
 * 格式: {salt}:{sm3Hash}
 */
public class SM3PasswordEncoder implements PasswordEncoder {

    private static final int SALT_LENGTH = 16;

    @Override
    public String encode(CharSequence rawPassword) {
        byte[] saltBytes = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(saltBytes);
        String salt = Base64.getEncoder().encodeToString(saltBytes);
        String hash = SmCryptoUtil.sm3Hash(salt + rawPassword.toString());
        return salt + ":" + hash;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null || !encodedPassword.contains(":")) {
            return false;
        }
        String[] parts = encodedPassword.split(":", 2);
        String salt = parts[0];
        String hash = parts[1];
        return hash.equals(SmCryptoUtil.sm3Hash(salt + rawPassword.toString()));
    }
}
