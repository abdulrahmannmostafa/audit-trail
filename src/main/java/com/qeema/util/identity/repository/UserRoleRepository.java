package com.qeema.util.identity.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qeema.util.identity.entity.UserRole;

public interface UserRoleRepository
        extends JpaRepository<UserRole, Integer> {

    List<UserRole> findByUserId(String userId);

    boolean existsByUserIdAndRoleId(
            String userId,
            String roleId);

    void deleteByUserIdAndRoleId(
            String userId,
            String roleId);
}