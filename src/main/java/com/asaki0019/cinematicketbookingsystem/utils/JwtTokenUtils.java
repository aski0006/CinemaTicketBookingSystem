package com.asaki0019.cinematicketbookingsystem.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import com.asaki0019.cinematicketbookingsystem.entities.User;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类，用于生成、解析、验证和刷新JWT Token，并从中提取用户信息。
 * 支持灵活的配置选项（如密钥、过期时间、签发者）以及异常处理机制。
 *
 * @author asaki0019
 */
public class JwtTokenUtils {
    // ====== 配置项 ======

    /**
     * 默认的JWT签名密钥，至少32位字符。
     */
    private static String SECRET_KEY = "AAAAAA-ASAKI--0019--AAAAAA-USST-WORK"; // 至少32位

    /**
     * Token默认过期时间，单位毫秒，默认为2小时。
     */
    private static long EXPIRATION = 1000 * 60 * 60 * 2; // 2小时，单位毫秒

    /**
     * 签发者标识，默认为"cinema-system"。
     */
    private static String ISSUER = "cinema-system";

    /**
     * 根据当前密钥生成对应的加密密钥对象。
     *
     * @return SecretKey 加密密钥
     */
    private static SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // ====== 1. 生成 JWT Token ======

    /**
     * 生成JWT Token。
     *
     * @param user 用户信息对象，包含id、用户名、手机号、邮箱、头像、会员等级、状态等信息
     * @return String 生成的JWT Token字符串
     */
    public static String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        claims.put("phone", user.getPhone());
        claims.put("email", user.getEmail());
        claims.put("avatar", user.getAvatar());
        claims.put("member_level", user.getMemberLevel());
        claims.put("status", user.getStatus());
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuer(ISSUER)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSecretKey())
                .compact();
    }

    // ====== 2. 解析 JWT Token ======

    /**
     * 解析JWT Token并获取其声明内容。
     *
     * @param token JWT Token字符串
     * @return Claims 包含所有声明信息的对象
     * @throws JwtException 如果token无效或签名不匹配
     */
    public static Claims parseToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ====== 3. 验证 JWT Token ======

    /**
     * 验证JWT Token是否有效（未过期）。
     *
     * @param token JWT Token字符串
     * @return boolean true表示有效，false表示无效
     */
    public static boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ====== 4. 获取 Token 中的用户信息 ======

    /**
     * 从JWT Token中提取用户信息。
     *
     * @param token JWT Token字符串
     * @return User 用户对象，包含id、用户名、手机号、邮箱、头像、会员等级、状态等信息
     */
    public static User getUserFromToken(String token) {
        Claims claims = parseToken(token);
        User user = new User();
        user.setId(claims.get("id") == null ? null : ((Number) claims.get("id")).longValue());
        user.setUsername((String) claims.get("username"));
        user.setPhone((String) claims.get("phone"));
        user.setEmail((String) claims.get("email"));
        user.setAvatar((String) claims.get("avatar"));
        user.setMemberLevel(claims.get("member_level") == null ? 0 : ((Number) claims.get("member_level")).intValue());
        user.setStatus((String) claims.get("status"));
        // password, createTime 不存入token
        return user;
    }

    // ====== 5. 刷新 JWT Token ======

    /**
     * 刷新JWT Token，使用原Token中的用户信息重新生成新的Token。
     *
     * @param token 原JWT Token字符串
     * @return String 新的JWT Token字符串
     */
    public static String refreshToken(String token) {
        User user = getUserFromToken(token);
        return generateToken(user);
    }

    // ====== 6. 灵活配置 ======

    /**
     * 设置JWT签名密钥。
     *
     * @param secretKey 新的密钥字符串
     */
    public static void setSecretKey(String secretKey) {
        SECRET_KEY = secretKey;
    }

    /**
     * 设置Token过期时间。
     *
     * @param expiration 新的过期时间，单位毫秒
     */
    public static void setExpiration(long expiration) {
        EXPIRATION = expiration;
    }

    /**
     * 设置签发者标识。
     *
     * @param issuer 新的签发者名称
     */
    public static void setIssuer(String issuer) {
        ISSUER = issuer;
    }

    // ====== 7. 异常处理（可自定义异常类） ======

    /**
     * 自定义JWT异常类，用于封装JWT操作过程中可能抛出的异常。
     */
    public static class JwtTokenException extends RuntimeException {
        /**
         * 构造函数，传入异常信息。
         *
         * @param message 异常信息
         */
        public JwtTokenException(String message) {
            super(message);
        }

        /**
         * 构造函数，传入异常信息和根本原因。
         *
         * @param message 异常信息
         * @param cause   根本原因
         */
        public JwtTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}