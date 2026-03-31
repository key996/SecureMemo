package com.securememo.app.crypto;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM 加密工具类
 *
 * 加密流程：
 *   1. 从图案密码派生 AES-256 密钥（SHA-256 哈希）
 *   2. 生成随机 12 字节 IV（GCM 推荐长度）
 *   3. AES/GCM/NoPadding 加密 JSON 明文
 *   4. 输出格式：Base64(IV + 密文 + GCM Tag)
 *
 * 解密流程：反向操作
 */
public class AesGcmCrypto {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;   // GCM 推荐 IV 长度（字节）
    private static final int GCM_TAG_LENGTH = 128;  // GCM 认证标签长度（位）
    private static final int KEY_LENGTH = 32;        // AES-256 密钥长度（字节）

    /**
     * 从图案密码字符串派生 AES-256 密钥
     * 使用 SHA-256 对密码哈希，得到 32 字节密钥
     *
     * @param patternPassword 图案密码的字符串表示（如 "0-1-2-4-7"）
     * @return AES SecretKey
     */
    public static SecretKey deriveKeyFromPattern(String patternPassword) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(patternPassword.getBytes(StandardCharsets.UTF_8));
        // SHA-256 输出恰好 32 字节，直接用作 AES-256 密钥
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 加密 JSON 字符串
     *
     * @param plainText 明文 JSON 字符串
     * @param key       AES-256 密钥
     * @return Base64 编码的密文（格式：IV(12B) + 密文 + GCM Tag(16B)）
     */
    public static String encrypt(String plainText, SecretKey key) throws Exception {
        // 生成随机 IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // 初始化 GCM 加密器
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

        // 执行加密
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 拼接 IV + 密文（GCM Tag 已包含在 cipherText 末尾）
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        // Base64 编码后返回
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    /**
     * 解密密文
     *
     * @param encryptedBase64 Base64 编码的密文
     * @param key             AES-256 密钥
     * @return 解密后的明文 JSON 字符串
     */
    public static String decrypt(String encryptedBase64, SecretKey key) throws Exception {
        byte[] combined = Base64.decode(encryptedBase64, Base64.NO_WRAP);

        // 提取 IV（前 12 字节）
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

        // 提取密文（剩余部分，含 GCM Tag）
        byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

        // 初始化 GCM 解密器
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

        // 执行解密（GCM 会自动验证完整性，若数据被篡改会抛出异常）
        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText, StandardCharsets.UTF_8);
    }

    /**
     * 计算图案密码的 SHA-256 哈希，用于存储验证
     *
     * @param patternPassword 图案密码字符串
     * @return 十六进制哈希字符串
     */
    public static String hashPattern(String patternPassword) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(patternPassword.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
