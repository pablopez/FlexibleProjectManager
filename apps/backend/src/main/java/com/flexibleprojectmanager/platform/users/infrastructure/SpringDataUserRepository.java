package com.flexibleprojectmanager.platform.users.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flexibleprojectmanager.platform.users.domain.User;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {
    boolean existsByEmail(String email);

    @Query("select u from UserJpaEntity u where exists (select m.id from OrganizationMemberJpaEntity m where m.userId = u.id and m.organizationId = :organizationId) and (:status is null or u.status = :status)")
    Page<UserJpaEntity> findByOrganizationId(@Param("organizationId") UUID organizationId,
                                               @Param("status") User.Status status, Pageable pageable);

    @Query("select u from UserJpaEntity u where u.id = :userId and exists (select m.id from OrganizationMemberJpaEntity m where m.userId = u.id and m.organizationId = :organizationId)")
    Optional<UserJpaEntity> findByOrganizationIdAndUserId(@Param("organizationId") UUID organizationId,
                                                           @Param("userId") UUID userId);
}
