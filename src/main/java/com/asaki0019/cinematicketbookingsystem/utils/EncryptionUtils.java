package com.asaki0019.cinematicketbookingsystem.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 加密工具类，主要用于用户密码的加密与校验，保障账户安全。
 */
public class EncryptionUtils {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 加密明文密码
     * 
     * @param rawPassword 明文密码
     * @return 加密后的密文
     */
    public static String encodePassword(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 校验明文密码与密文是否匹配
     * 
     * @param rawPassword     明文密码
     * @param encodedPassword 密文
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}