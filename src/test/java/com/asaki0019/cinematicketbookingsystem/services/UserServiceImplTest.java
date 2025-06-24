package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.User;
import com.asaki0019.cinematicketbookingsystem.repository.UserRepository;
import com.asaki0019.cinematicketbookingsystem.utils.EncryptionUtils;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private MockedStatic<EncryptionUtils> encryptionUtilsMock;
    private MockedStatic<JwtTokenUtils> jwtTokenUtilsMock;
    private User testUser;

    @BeforeEach
    void setUp() {
        encryptionUtilsMock = mockStatic(EncryptionUtils.class);
        jwtTokenUtilsMock = mockStatic(JwtTokenUtils.class);
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setStatus("ACTIVE");
    }

    @AfterEach
    void tearDown() {
        encryptionUtilsMock.close();
        jwtTokenUtilsMock.close();
    }

    @Test
    void register_Success() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        encryptionUtilsMock.when(() -> EncryptionUtils.encodePassword(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);
            userToSave.setId(1L); // Simulate DB generating an ID
            return userToSave;
        });

        Map<String, Object> result = userService.register("test", "pass", "123", "test@test.com");
        assertNotNull(result.get("id"));
    }

    @Test
    void register_UserExists() {
        when(userRepository.existsByUsername("test")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> userService.register("test", "pass", "123", "test@test.com"));
    }

    @Test
    void login_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        encryptionUtilsMock.when(() -> EncryptionUtils.matches("password", "encodedPassword")).thenReturn(true);
        jwtTokenUtilsMock.when(() -> JwtTokenUtils.generateToken(any(User.class))).thenReturn("fake-token");

        Map<String, Object> result = userService.login("testuser", "password");
        assertEquals("fake-token", result.get("token"));
    }

    @Test
    void login_UserNotFound() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> userService.login("testuser", "password"));
    }

    @Test
    void login_WrongPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        encryptionUtilsMock.when(() -> EncryptionUtils.matches(anyString(), anyString())).thenReturn(false);
        assertThrows(RuntimeException.class, () -> userService.login("testuser", "wrongpassword"));
    }

    @Test
    void updateUserInfo_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = userService.updateUserInfo(1L, "newphone", "new@email.com", "new_avatar");
        assertNotNull(result);
        assertEquals(1L, result.get("id"));
    }

    @Test
    void updateUserInfo_PhoneConflict() {
        User conflictingUser = new User();
        conflictingUser.setId(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByPhone("newphone")).thenReturn(Optional.of(conflictingUser));

        assertThrows(ResponseStatusException.class, () -> userService.updateUserInfo(1L, "newphone", null, null));
    }
}