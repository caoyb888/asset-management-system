declare module 'sm-crypto' {
  export const sm2: {
    doEncrypt(msgHex: string, publicKey: string, cipherMode?: number): string
    doDecrypt(cipherHex: string, privateKey: string, cipherMode?: number): string
  }
  export const sm3: {
    (msg: string): string
  }
  export const sm4: {
    encrypt(msg: string, key: string): string
    decrypt(cipherText: string, key: string): string
  }
}
