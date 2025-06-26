package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.User;
import com.asaki0019.cinematicketbookingsystem.repository.UserRepository;
import com.asaki0019.cinematicketbookingsystem.utils.EncryptionUtils;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Map<String, Object> register(String username, String password, String phone, String email) {
        Map<String, Object> resp = new HashMap<>();
        // 检查唯一性
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) || !StringUtils.hasText(phone)
                || !StringUtils.hasText(email)) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        if (findByPhone(phone) != null) {
            throw new RuntimeException("手机号已存在");
        }
        if (existsByEmail(email)) {
            throw new RuntimeException("邮箱已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(EncryptionUtils.encodePassword(password));
        user.setPhone(phone);
        user.setEmail(email);
        user.setMemberLevel(0);
        user.setStatus("ACTIVE");
        user.setCreateTime(LocalDateTime.now());
        user = userRepository.save(user);

        resp.put("id", user.getId());
        resp.put("member_level", user.getMemberLevel());
        resp.put("create_time", user.getCreateTime());
        return resp;
    }

    @Override
    public Map<String, Object> login(String identifier, String password) {
        User user = findByUsername(identifier);
        if (user == null) {
            user = findByPhone(identifier);
        }
        if (user == null) {
            user = findByEmail(identifier);
        }
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!EncryptionUtils.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("账号状态异常");
        }

        String token = JwtTokenUtils.generateToken(user);
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("member_level", user.getMemberLevel());

        Map<String, Object> resp = new HashMap<>();
        resp.put("token", token);
        resp.put("user", userMap);
        return resp;
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public User findByPhone(String phone) {
        // 添加到UserRepository中
        return userRepository.findByPhone(phone).orElse(null);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Map<String, Object> updateUserInfo(Long userId, String phone, String email, String avatar) {
        Map<String, Object> resp = new HashMap<>();

        // 获取用户
        User user = getUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        // 验证手机号唯一性
        if (phone != null && !phone.equals(user.getPhone())) {
            User existingUser = findByPhone(phone);
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "手机号已被使用");
            }
            user.setPhone(phone);
        }

        // 验证邮箱唯一性
        if (email != null && !email.equals(user.getEmail())) {
            if (existsByEmail(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已被使用");
            }
            user.setEmail(email);
        }

        // 更新头像
        if (avatar != null) {
            user.setAvatar(avatar);
        }

        // 保存更新
        user = updateUser(user);

        // 返回结果
        resp.put("id", user.getId());
        resp.put("update_time", LocalDateTime.now());
        return resp;
    }
}