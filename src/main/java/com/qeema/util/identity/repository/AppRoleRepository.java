package com.qeema.util.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qeema.util.identity.entity.AppRole;

public interface AppRoleRepository
        extends JpaRepository<AppRole, Integer> {

    Optional<AppRole> findByName(String name);

    boolean existsByName(String name);
}