package com.qeema.util.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qeema.util.notification.entity.EmailLog;

public interface EmailLogRepository extends JpaRepository<EmailLog, String> {
}
