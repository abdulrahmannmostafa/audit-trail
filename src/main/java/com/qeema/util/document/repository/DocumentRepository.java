package com.qeema.util.document.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.qeema.util.document.entity.Document;

public interface DocumentRepository extends JpaRepository<Document, String> {

        @Query("""
                        select distinct d from Document d
                        left join DocumentPermission p on p.documentId = d.id
                        where d.status = 'ACTIVE'
                          and (d.ownerId = :userId
                               or (p.subjectType = 'USER' and p.subjectId = :userId
                                   and p.permission in ('READ', 'EDIT', 'OWNER')))
                        order by d.updatedAt desc
                        """)
        List<Document> findAccessible(@Param("userId") String userId);

        @Query("""
                        select distinct d from Document d
                        left join DocumentPermission p on p.documentId = d.id
                        where d.id = :documentId
                          and d.status = 'ACTIVE'
                          and (d.ownerId = :userId
                               or (p.subjectType = 'USER' and p.subjectId = :userId
                                   and p.permission in ('READ', 'EDIT', 'OWNER')))
                        """)
        Optional<Document> findAccessibleById(
                        @Param("documentId") String documentId,
                        @Param("userId") String userId);
}
