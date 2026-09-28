package com.flexibleprojectmanager.platform.users.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flexibleprojectmanager.platform.users.domain.OrganizationMember;
import com.flexibleprojectmanager.platform.users.domain.User;

public interface SpringDataOrganizationMemberRepository extends JpaRepository<OrganizationMemberJpaEntity, UUID> {
    Optional<OrganizationMemberJpaEntity> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    @Query("select count(m.id) from OrganizationMemberJpaEntity m, UserJpaEntity u join m.roles r where m.organizationId = :organizationId and m.userId = u.id and m.status = :memberStatus and u.status = :userStatus and r.code = 'ADMIN' and m.userId <> :excludedUserId")
    long countOtherEffectiveActiveAdmins(@Param("organizationId") UUID organizationId,
                                         @Param("excludedUserId") UUID excludedUserId,
                                         @Param("memberStatus") OrganizationMember.Status memberStatus,
                                         @Param("userStatus") User.Status userStatus);
}
