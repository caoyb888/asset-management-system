package com.asset.common.security.utils;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.Security;

/**
 * 国密算法工具类
 * SM2: 非对称加密(传输加密) - 前端公钥加密, 后端私钥解密
 * SM3: 哈希摘要(密码存储) - 替代SHA/MD5
 * SM4: 对称加密(存储加密) - 敏感字段加密存储
 */
public final class SmCryptoUtil {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private SmCryptoUtil() {}

    // ==================== SM3 哈希 ====================

    /**
     * SM3哈希
     */
    public static String sm3Hash(String input) {
        SM3Digest digest = new SM3Digest();
        byte[] data = input.getBytes(StandardCharsets.UTF_8);
        digest.update(data, 0, data.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return Hex.toHexString(hash);
    }

    // ==================== SM4 对称加密 ====================

    /**
     * SM4加密(CBC模式) - 用于敏感字段(手机号/身份证/银行卡)存储加密
     */
    public static String sm4Encrypt(String plainText, String key, String iv) {
        try {
            byte[] keyBytes = Hex.decode(key);
            byte[] ivBytes = Hex.decode(iv);
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                    CBCBlockCipher.newInstance(new SM4Engine()));
            cipher.init(true, new ParametersWithIV(new KeyParameter(keyBytes), ivBytes));
            byte[] input = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] output = new byte[cipher.getOutputSize(input.length)];
            int len = cipher.processBytes(input, 0, input.length, output, 0);
            len += cipher.doFinal(output, len);
            byte[] result = new byte[len];
            System.arraycopy(output, 0, result, 0, len);
            return Hex.toHexString(result);
        } catch (Exception e) {
            throw new RuntimeException("SM4加密失败", e);
        }
    }

    /**
     * SM4解密(CBC模式)
     */
    public static String sm4Decrypt(String cipherText, String key, String iv) {
        try {
            byte[] keyBytes = Hex.decode(key);
            byte[] ivBytes = Hex.decode(iv);
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                    CBCBlockCipher.newInstance(new SM4Engine()));
            cipher.init(false, new ParametersWithIV(new KeyParameter(keyBytes), ivBytes));
            byte[] input = Hex.decode(cipherText);
            byte[] output = new byte[cipher.getOutputSize(input.length)];
            int len = cipher.processBytes(input, 0, input.length, output, 0);
            len += cipher.doFinal(output, len);
            return new String(output, 0, len, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("SM4解密失败", e);
        }
    }
}
