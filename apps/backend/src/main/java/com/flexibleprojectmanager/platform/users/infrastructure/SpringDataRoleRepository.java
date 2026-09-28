package com.flexibleprojectmanager.platform.users.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataRoleRepository extends JpaRepository<RoleJpaEntity, UUID> {
    List<RoleJpaEntity> findByCodeIn(Collection<String> codes);
    List<RoleJpaEntity> findByCodeInAndSystemDefinedTrue(Collection<String> codes);
    List<RoleJpaEntity> findBySystemDefinedTrueOrderByCodeAsc();
    Optional<RoleJpaEntity> findByCode(String code);
}
