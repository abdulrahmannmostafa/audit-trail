package com.qeema.util.identity.repository;

import com.qeema.util.identity.entity.UserIdentity;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserIdentityRepository
        extends JpaRepository<UserIdentity, String> {

    @Query("""
            SELECT i
            FROM UserIdentity i
            JOIN FETCH i.user
            WHERE i.issuer = :issuer
              AND i.subject = :subject
            """)
    Optional<UserIdentity> findByIssuerAndSubjectWithUser(
            @Param("issuer") String issuer,
            @Param("subject") String subject);
}