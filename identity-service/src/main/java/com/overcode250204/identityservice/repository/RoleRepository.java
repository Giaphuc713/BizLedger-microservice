package com.overcode250204.identityservice.repository;

import com.overcode250204.identityservice.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByTenantIdAndCode(UUID tenantId, String code);

    List<Role> findByTenantId(UUID tenantId);
}
