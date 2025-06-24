package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.User;
import com.asaki0019.cinematicketbookingsystem.repository.AdminUserRepository;
import com.asaki0019.cinematicketbookingsystem.utils.LogSystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserServiceImpl implements AdminUserService {
    @Autowired
    private AdminUserRepository adminUserRepository;

    @Override
    public List<User> getUserList(String status, int page, int size) {
        // 简单实现：不区分status，实际可用Specification等动态查询
        return adminUserRepository.findAll(PageRequest.of(page - 1, size)).getContent();
    }

    @Override
    public User createUser(User user) {
        if (!com.asaki0019.cinematicketbookingsystem.utils.ValidationUtils.validateUser(user)) {
            throw new IllegalArgumentException("用户参数不合法");
        }
        return adminUserRepository.save(user);
    }

    @Override
    public User updateUser(Long userId, User user) {
        if (!com.asaki0019.cinematicketbookingsystem.utils.ValidationUtils.validateUser(user)) {
            throw new IllegalArgumentException("用户参数不合法");
        }
        user.setId(userId);
        return adminUserRepository.save(user);
    }

    @Override
    public void deleteUser(Long userId) {
        adminUserRepository.deleteById(userId);
    }

    @Override
    public User lockUser(Long userId, String reason) {
        User user = adminUserRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setStatus("LOCKED");
            LogSystem.info(user.getUsername() + "is Locked by " + reason);
            adminUserRepository.save(user);
        }
        return user;
    }

    @Override
    public User unlockUser(Long userId) {
        User user = adminUserRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setStatus("ACTIVE");
            LogSystem.info(user.getUsername() + "is Unlocked");
            adminUserRepository.save(user);
        }
        return user;
    }
}