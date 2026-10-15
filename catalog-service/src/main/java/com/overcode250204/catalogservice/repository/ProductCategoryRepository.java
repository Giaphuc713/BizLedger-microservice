package com.overcode250204.catalogservice.repository;

import com.overcode250204.catalogservice.entity.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {
    Page<ProductCategory> findByTenantIdAndActive(UUID tenantId, boolean active, Pageable pageable);

    List<ProductCategory> findByTenantIdAndActive(UUID tenantId, boolean active);

    Optional<ProductCategory> findByIdAndTenantId(UUID id, UUID tenantId);
}
