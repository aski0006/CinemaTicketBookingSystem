package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.entities.User;
import com.asaki0019.cinematicketbookingsystem.services.AdminUserService;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {
    @Autowired
    private AdminUserService adminUserService;

    private boolean checkJwt(String token) {
        return token != null && JwtTokenUtils.validateToken(token);
    }

    @GetMapping
    public Map<String, Object> getUserList(@RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> resp = new HashMap<>();
        if (!checkJwt(token)) {
            resp.put("error", "未登录或token已过期");
            return resp;
        }
        List<User> users = adminUserService.getUserList(status, page, size);
        resp.put("users", users);
        resp.put("total", users.size());
        return resp;
    }

    @PostMapping
    public Object createUser(@RequestHeader("Authorization") String token, @RequestBody User user) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        return adminUserService.createUser(user);
    }

    @PutMapping("/{userId}")
    public Object updateUser(@RequestHeader("Authorization") String token,
            @PathVariable Long userId,
            @RequestBody User user) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        return adminUserService.updateUser(userId, user);
    }

    @DeleteMapping("/{userId}")
    public Object deleteUser(@RequestHeader("Authorization") String token,
            @PathVariable Long userId) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        adminUserService.deleteUser(userId);
        return Map.of("success", true);
    }

    @PostMapping("/{userId}/lock")
    public Object lockUser(@RequestHeader("Authorization") String token,
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, String> req) {
        if (!checkJwt(token)) {
            return Map.of("error", "未登录或token已过期");
        }
        String reason = req != null ? req.getOrDefault("reason", "") : "";
        return adminUserService.lockUser(userId, reason);
    }
}