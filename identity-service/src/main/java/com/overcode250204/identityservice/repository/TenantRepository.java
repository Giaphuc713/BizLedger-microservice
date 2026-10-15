package com.overcode250204.identityservice.repository;

import com.overcode250204.identityservice.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    boolean existsByCode(String code);

    Optional<Tenant> findByCode(String code);
}
