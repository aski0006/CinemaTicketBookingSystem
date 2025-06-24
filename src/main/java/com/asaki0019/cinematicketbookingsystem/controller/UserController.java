package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.entities.User;
import com.asaki0019.cinematicketbookingsystem.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> req) {
        Map<String, Object> resp = new HashMap<>();
        try {
            Map<String, Object> data = userService.register(
                    req.get("username"), req.get("password"), req.get("phone"), req.get("email"));
            resp.putAll(data);
            return resp;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("已存在")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> req) {
        Map<String, Object> resp = new HashMap<>();
        try {
            Map<String, Object> data = userService.login(req.get("identifier"), req.get("password"));
            resp.putAll(data);
            return resp;
        } catch (RuntimeException e) {
            if (e.getMessage().contains("密码错误") || e.getMessage().contains("账号状态异常")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 修改个人信息
     */
    @PutMapping("/{userId}")
    public Map<String, Object> updateUserInfo(@PathVariable Long userId, @RequestBody Map<String, String> req) {
        try {
            return userService.updateUserInfo(userId, req.get("phone"), req.get("email"), req.get("avatar"));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "更新失败：" + e.getMessage());
        }
    }
}