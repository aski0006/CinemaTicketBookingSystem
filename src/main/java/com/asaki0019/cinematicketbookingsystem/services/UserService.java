package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserService {
    /**
     * 用户注册，返回用户id和create_time
     */
    Map<String, Object> register(String username, String password, String phone, String email);

    /**
     * 用户登录，返回token和user对象
     */
    Map<String, Object> login(String identifier, String password);

    User findByUsername(String username);

    User findByPhone(String phone);

    User findByEmail(String email);

    User saveUser(User user);

    Optional<User> getUserById(Long id);

    Optional<User> getUserByUsername(String username);

    Optional<User> getUserByEmail(String email);

    List<User> getAllUsers();

    User updateUser(User user);

    void deleteUser(Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}