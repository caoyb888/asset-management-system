package com.asset.common.security.crypto;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * 国密 SM2 / SM3 工具类
 * <p>
 * SM2：非对称加密，用于登录密码传输（前端公钥加密，后端私钥解密）<br>
 * SM3：哈希算法，用于密码存储
 * </p>
 */
public final class SmCryptoUtil {

    /** SM2 曲线参数 */
    private static final X9ECParameters SM2_CURVE = GMNamedCurves.getByName("sm2p256v1");
    private static final ECDomainParameters DOMAIN_PARAMS = new ECDomainParameters(
            SM2_CURVE.getCurve(), SM2_CURVE.getG(), SM2_CURVE.getN(), SM2_CURVE.getH());

    /**
     * 后端 SM2 私钥（Hex）
     * 对应前端 sm-crypto 中配置的公钥
     */
    public static final String PRIVATE_KEY_HEX =
            "BF504AF9D74A8E072F09D4A06376E3337EA5562E12056D3B1410B53214E9E27A";

    /**
     * 后端 SM2 公钥（Hex，04 开头非压缩格式）
     * 前端 sm-crypto 中的 SM2_PUBLIC_KEY 需设置为此值
     */
    public static final String PUBLIC_KEY_HEX =
            "04FCF0FF361BB00B2391CC9A77F72CD42F2D9DCAD872CDBA8420C44746EBC0D4A" +
            "2FD59544B0539DF7AEFA2BEBB5CABC543BA40BA920A0B037C015A684700A14CB5";

    private SmCryptoUtil() {}

    /**
     * SM2 解密（C1C3C2 模式，对应前端 sm2.doEncrypt 的 mode=1）
     *
     * @param cipherHex 前端加密后的 hex 字符串（含 04 前缀）
     * @return 明文字符串
     */
    public static String sm2Decrypt(String cipherHex) {
        try {
            // sm-crypto 前端库输出的密文不含 04 非压缩点前缀，BouncyCastle 解密需要补上
            if (!cipherHex.toLowerCase().startsWith("04")) {
                cipherHex = "04" + cipherHex;
            }
            byte[] cipherBytes = Hex.decode(cipherHex);
            BigInteger d = new BigInteger(PRIVATE_KEY_HEX, 16);
            ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(d, DOMAIN_PARAMS);

            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(false, privKey);
            byte[] plainBytes = engine.processBlock(cipherBytes, 0, cipherBytes.length);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (InvalidCipherTextException e) {
            throw new IllegalArgumentException("SM2 解密失败", e);
        }
    }

    /**
     * SM3 哈希（用于密码存储）
     *
     * @param input 明文
     * @return SM3 哈希 hex 字符串（64位）
     */
    public static String sm3Hash(String input) {
        org.bouncycastle.crypto.digests.SM3Digest digest = new org.bouncycastle.crypto.digests.SM3Digest();
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        digest.update(bytes, 0, bytes.length);
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return Hex.toHexString(result);
    }

    /**
     * 验证密码（明文 vs SM3 哈希）
     */
    public static boolean sm3Matches(String rawPassword, String storedHash) {
        return sm3Hash(rawPassword).equalsIgnoreCase(storedHash);
    }
}
