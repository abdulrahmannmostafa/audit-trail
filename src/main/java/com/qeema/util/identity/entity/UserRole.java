package com.qeema.util.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "user_role", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "role_id" }))
@Getter
@Setter
@NoArgsConstructor
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private AppRole role;

    @Column(name = "assigned_at", insertable = false, updatable = false)
    private Instant assignedAt;

    @Builder
    public UserRole(User user, AppRole role) {
        this.user = user;
        this.role = role;
    }

    @Builder
    public UserRole(User user, AppRole role, Instant assignedAt) {
        this.user = user;
        this.role = role;
        this.assignedAt = assignedAt;
    }

    @Override
    public String toString() {
        return "UserRole{" + "id=" + id + ", user=" + (user != null ? user.getId() : null) + ", role="
                + (role != null ? role.getId() : null) + ", assignedAt=" + assignedAt + '}';
    }
}