package com.qeema.util.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class UserRoleId implements Serializable {

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    public UserRoleId() {
    }

    public UserRoleId(String userId, Integer roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof UserRoleId that))
            return false;
        return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }

    @Override
    public String toString() {
        return "UserRoleId{" + "userId='" + userId + '\'' + ", roleId=" + roleId + '}';
    }
}