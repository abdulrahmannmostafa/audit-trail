package com.qeema.util.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.Builder;

interface Status {
    String ACTIVE = "ACTIVE";
    String DISABLED = "DISABLED";
}

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "display_name", length = 120, nullable = false)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "status", length = 20, nullable = false)
    private String status = Status.ACTIVE;

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

    @Builder
    public User(String email, String displayName, Optional<String> avatarUrl) {
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl.orElse(null);
    }

    @Override
    public String toString() {
        return "User{" + "id='" + id + '\'' + ", email='" + email + '\'' + ", displayName='" + displayName + '\''
                + ", avatarUrl='" + avatarUrl + '\'' + ", status='" + status + '\'' + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt + '}';
    }
}