package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.UserListResponseDTO;
import com.asaki0019.cinematicketbookingsystem.dto.UserResponseDTO;
import com.asaki0019.cinematicketbookingsystem.entities.User;
import com.asaki0019.cinematicketbookingsystem.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Autowired
    private AdminUserRepository adminUserRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserListResponseDTO getAllUsers(Pageable pageable, String status) {
        Page<User> userPage;
        if (status != null && !status.isEmpty()) {
            userPage = adminUserRepository.findByStatus(status, pageable);
        } else {
            userPage = adminUserRepository.findAll(pageable);
        }

        List<UserResponseDTO> userDTOs = userPage.getContent().stream()
                .map(this::convertToUserResponseDTO)
                .collect(Collectors.toList());

        return new UserListResponseDTO(userPage.getTotalElements(), userDTOs);
    }

    private UserResponseDTO convertToUserResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getEmail(),
                user.getAvatar(),
                user.getMemberLevel(),
                user.getStatus(),
                user.getCreateTime());
    }

    @Override
    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        if (user.getStatus() == null) {
            user.setStatus("ACTIVE");
        }
        return adminUserRepository.save(user);
    }

    @Override
    public User updateUser(Long id, User userDetails) {
        User user = adminUserRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (userDetails.getUsername() != null)
            user.setUsername(userDetails.getUsername());
        if (userDetails.getEmail() != null)
            user.setEmail(userDetails.getEmail());
        if (userDetails.getPhone() != null)
            user.setPhone(userDetails.getPhone());
        if (userDetails.getAvatar() != null)
            user.setAvatar(userDetails.getAvatar());
        if (userDetails.getStatus() != null)
            user.setStatus(userDetails.getStatus());
        if (userDetails.getMemberLevel() != null && userDetails.getMemberLevel() != 0)
            user.setMemberLevel(userDetails.getMemberLevel());
        return adminUserRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        adminUserRepository.deleteById(id);
    }

    @Override
    public User lockUser(Long userId) {
        User user = adminUserRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus("LOCKED");
        return adminUserRepository.save(user);
    }

    @Override
    public User unlockUser(Long userId) {
        User user = adminUserRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus("ACTIVE");
        return adminUserRepository.save(user);
    }
}