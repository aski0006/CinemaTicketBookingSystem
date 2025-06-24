package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.User;
import java.util.List;

public interface AdminUserService {
    List<User> getUserList(String status, int page, int size);

    User createUser(User user);

    User updateUser(Long userId, User user);

    void deleteUser(Long userId);

    User lockUser(Long userId, String reason);

    User unlockUser(Long userId);
}