package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.UserListResponseDTO;
import com.asaki0019.cinematicketbookingsystem.entities.User;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    UserListResponseDTO getAllUsers(Pageable pageable, String status);

    User createUser(User user);

    User updateUser(Long id, User userDetails);

    void deleteUser(Long id);

    User lockUser(Long userId);

    User unlockUser(Long userId);
}