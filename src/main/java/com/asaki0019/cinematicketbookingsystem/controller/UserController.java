package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.entities.User;
import com.asaki0019.cinematicketbookingsystem.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.asaki0019.cinematicketbookingsystem.dto.UserResponseDTO;
import com.asaki0019.cinematicketbookingsystem.services.OrderService;
import com.asaki0019.cinematicketbookingsystem.services.SessionService;
import com.asaki0019.cinematicketbookingsystem.entities.Movie;
import com.asaki0019.cinematicketbookingsystem.repository.MovieRepository;
import com.asaki0019.cinematicketbookingsystem.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private OrderRepository orderRepository;

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

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication
                .getPrincipal() instanceof com.asaki0019.cinematicketbookingsystem.entities.User user)) {
            return ResponseEntity.status(401).build();
        }
        // 查数据库最新用户信息
        java.util.Optional<User> dbUserOpt = userService.getUserById(user.getId());
        if (dbUserOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        User dbUser = dbUserOpt.get();
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(dbUser.getId());
        dto.setUsername(dbUser.getUsername());
        dto.setPhone(dbUser.getPhone());
        dto.setEmail(dbUser.getEmail());
        dto.setAvatar(dbUser.getAvatar());
        dto.setMemberLevel(dbUser.getMemberLevel());
        dto.setStatus(dbUser.getStatus());
        dto.setCreateTime(dbUser.getCreateTime());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/my-today-sessions")
    public Object getMyTodaySessions(@RequestHeader(name = "Authorization", required = false) String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return List.of();
        }
        token = token.substring(7);
        if (!com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils.validateToken(token)) {
            return List.of();
        }
        Long userId = null;
        try {
            userId = com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils.getUserFromToken(token).getId();
        } catch (Exception e) {
            return List.of();
        }
        if (userId == null)
            return List.of();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime start = today.atStartOfDay();
        java.time.LocalDateTime end = today.plusDays(1).atStartOfDay();
        java.util.List<com.asaki0019.cinematicketbookingsystem.entities.Order> orders = orderRepository
                .findByUserId(userId).stream()
                .filter(o -> o.getStatus() != null && !"CANCELLED".equals(o.getStatus())
                        && !"FAILED".equals(o.getStatus()))
                .filter(o -> o.getCreateTime() != null && !o.getCreateTime().isBefore(start)
                        && o.getCreateTime().isBefore(end))
                .toList();
        java.util.Set<Long> sessionIds = orders.stream()
                .map(com.asaki0019.cinematicketbookingsystem.entities.Order::getSessionId)
                .collect(java.util.stream.Collectors.toSet());
        java.util.List<Object> result = new java.util.ArrayList<>();
        for (Long sessionId : sessionIds) {
            com.asaki0019.cinematicketbookingsystem.entities.Session session = null;
            try {
                session = sessionService.getSessionByIdWithRedis(sessionId);
            } catch (Exception ignore) {
            }
            if (session != null) {
                com.asaki0019.cinematicketbookingsystem.entities.Movie movie = null;
                try {
                    movie = movieRepository.findById(session.getMovieId()).orElse(null);
                } catch (Exception ignore) {
                }
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", session.getId());
                map.put("movieTitle", movie != null ? movie.getTitle() : "未知影片");
                map.put("startTime", session.getStartTime());
                map.put("endTime", session.getEndTime());
                map.put("hallName", "" + session.getHallId());
                map.put("price", session.getPrice());
                result.add(map);
            }
        }
        return result;
    }
}