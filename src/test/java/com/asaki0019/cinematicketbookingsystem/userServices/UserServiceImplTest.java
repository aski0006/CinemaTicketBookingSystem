package com.asaki0019.cinematicketbookingsystem.userServices;

import com.asaki0019.cinematicketbookingsystem.services.UserService;
import com.asaki0019.cinematicketbookingsystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UserServiceImplTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testRegisterAndLogin() {
        // 1. 注册
        String username = "testuser" + System.currentTimeMillis();
        String email = username + "@example.com";
        String phone = String.valueOf(System.currentTimeMillis() / 1000);
        Map<String, Object> registerResult = userService.register(username, "password123", phone, email);
        assertNotNull(registerResult.get("id"));

        // 2. 登录
        Map<String, Object> loginResult = userService.login(username, "password123");
        assertNotNull(loginResult.get("token"));
        assertTrue(loginResult.get("token") instanceof String);
    }

    @Test
    void testUpdateUserInfo() {
        // 1. 先注册一个用户
        String username = "updateuser" + System.currentTimeMillis();
        String email = username + "@example.com";
        String phone = String.valueOf(System.currentTimeMillis() / 1000);
        Map<String, Object> registerResult = userService.register(username, "password123", phone, email);
        Long userId = (Long) registerResult.get("id");

        // 2. 更新用户信息
        String newPhone = String.valueOf(System.currentTimeMillis() / 1000 + 1);
        String newEmail = "updated." + email;
        Map<String, Object> updateResult = userService.updateUserInfo(userId, newPhone, newEmail, "new_avatar_url");

        assertNotNull(updateResult.get("id"));

        // 3. 验证更新
        Map<String, Object> loginResult = userService.login(username, "password123");
        Map<String, Object> userMap = (Map<String, Object>) loginResult.get("user");
        assertEquals(userId, userMap.get("id"));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }
}