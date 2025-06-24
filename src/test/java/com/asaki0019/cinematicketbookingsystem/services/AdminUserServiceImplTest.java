package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.UserListResponseDTO;
import com.asaki0019.cinematicketbookingsystem.entities.User;
import com.asaki0019.cinematicketbookingsystem.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setMemberLevel(1);
        user.setStatus("ACTIVE");
        return user;
    }

    @Test
    void getAllUsers() {
        Page<User> userPage = new PageImpl<>(Collections.singletonList(createTestUser()));
        when(adminUserRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        UserListResponseDTO result = adminUserService.getAllUsers(Pageable.unpaged(), null);

        assertEquals(1, result.getTotal());
        assertFalse(result.getUsers().isEmpty());
    }

    @Test
    void getAllUsersByStatus() {
        Page<User> userPage = new PageImpl<>(Collections.singletonList(createTestUser()));
        when(adminUserRepository.findByStatus(eq("ACTIVE"), any(Pageable.class))).thenReturn(userPage);

        UserListResponseDTO result = adminUserService.getAllUsers(Pageable.unpaged(), "ACTIVE");

        assertEquals(1, result.getTotal());
    }

    @Test
    void createUser() {
        User user = new User();
        user.setPassword("password");
        when(adminUserRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User result = adminUserService.createUser(user);

        assertNotNull(result.getPassword());
        assertNotEquals("password", result.getPassword());
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    void updateUser() {
        User existingUser = createTestUser();
        User userDetails = new User();
        userDetails.setUsername("newUsername");
        userDetails.setMemberLevel(2);

        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(adminUserRepository.save(any(User.class))).thenReturn(existingUser);

        User result = adminUserService.updateUser(1L, userDetails);

        assertEquals("newUsername", result.getUsername());
        assertEquals(2, result.getMemberLevel());
    }

    @Test
    void updateUser_NotFound() {
        when(adminUserRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> adminUserService.updateUser(1L, new User()));
    }

    @Test
    void deleteUser() {
        adminUserService.deleteUser(1L);
        verify(adminUserRepository).deleteById(1L);
    }

    @Test
    void lockUser() {
        User user = new User();
        user.setStatus("ACTIVE");
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(adminUserRepository.save(any(User.class))).thenReturn(user);

        User result = adminUserService.lockUser(1L);

        assertEquals("LOCKED", result.getStatus());
    }

    @Test
    void unlockUser() {
        User user = new User();
        user.setStatus("LOCKED");
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(adminUserRepository.save(any(User.class))).thenReturn(user);

        User result = adminUserService.unlockUser(1L);

        assertEquals("ACTIVE", result.getStatus());
    }
}