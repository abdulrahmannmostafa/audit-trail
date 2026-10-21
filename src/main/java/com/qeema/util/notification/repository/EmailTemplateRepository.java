package com.qeema.util.notification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qeema.util.notification.entity.EmailTemplate;

public interface EmailTemplateRepository
        extends JpaRepository<EmailTemplate, String> {

    Optional<EmailTemplate> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, String id);
}
