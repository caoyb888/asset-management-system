import { sm2, sm4 } from 'sm-crypto'

/** SM2 服务端公钥（与后端 SmCryptoUtil 对应，上线前替换为实际值） */
const SM2_PUBLIC_KEY =
  '04FCF0FF361BB00B2391CC9A77F72CD42F2D9DCAD872CDBA8420C44746EBC0D4A' +
  '2FD59544B0539DF7AEFA2BEBB5CABC543BA40BA920A0B037C015A684700A14CB5'

/**
 * SM2 加密（用于登录密码传输）
 */
export function sm2Encrypt(plainText: string): string {
  return sm2.doEncrypt(plainText, SM2_PUBLIC_KEY, 1)
}

/** SM4 密钥（16字节十六进制，与后端一致） */
const SM4_KEY = '0123456789abcdeffedcba9876543210'

/**
 * SM4 加密
 */
export function sm4Encrypt(plainText: string): string {
  return sm4.encrypt(plainText, SM4_KEY)
}

/**
 * SM4 解密
 */
export function sm4Decrypt(cipherText: string): string {
  return sm4.decrypt(cipherText, SM4_KEY)
}
