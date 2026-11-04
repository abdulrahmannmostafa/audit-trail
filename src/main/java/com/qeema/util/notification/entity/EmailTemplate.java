package com.qeema.util.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "email_template")
@Getter
@Setter
public class EmailTemplate {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "subject_template", length = 255, nullable = false)
    private String subjectTemplate;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "html_content", columnDefinition = "TEXT", nullable = false)
    private String htmlContent;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public EmailTemplate() {
    }

    public EmailTemplate(String name, String subjectTemplate, String htmlContent, boolean enabled) {
        this.name = name;
        this.subjectTemplate = subjectTemplate;
        this.htmlContent = htmlContent;
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "EmailTemplate{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", subjectTemplate='"
                + subjectTemplate + '\'' + ", htmlContent='" + htmlContent + '\'' + ", enabled=" + enabled
                + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + '}';
    }
}