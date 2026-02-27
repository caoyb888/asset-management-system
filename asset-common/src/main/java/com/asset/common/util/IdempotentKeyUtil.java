package com.asset.common.util;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 幂等键生成工具类
 * 格式：{prefix}_{id}_{version}_{md5_hash前8位}
 */
public final class IdempotentKeyUtil {

    private IdempotentKeyUtil() {}

    /**
     * 生成幂等键
     *
     * @param prefix  业务前缀（如 "receivable", "ledger"）
     * @param id      业务主键
     * @param version 版本号
     * @return 幂等键字符串，格式：{prefix}_{id}_{version}_{hash8}
     */
    public static String generate(String prefix, Long id, Integer version) {
        String raw = prefix + "_" + id + "_" + version;
        String hash = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8)).substring(0, 8);
        return raw + "_" + hash;
    }

    /**
     * 生成幂等键（带额外盐值）
     *
     * @param prefix    业务前缀
     * @param id        业务主键
     * @param version   版本号
     * @param extraSalt 额外盐值（如时间戳、随机字符串）
     * @return 幂等键字符串
     */
    public static String generate(String prefix, Long id, Integer version, String extraSalt) {
        String raw = prefix + "_" + id + "_" + version + "_" + extraSalt;
        String hash = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8)).substring(0, 8);
        return prefix + "_" + id + "_" + version + "_" + hash;
    }
}
