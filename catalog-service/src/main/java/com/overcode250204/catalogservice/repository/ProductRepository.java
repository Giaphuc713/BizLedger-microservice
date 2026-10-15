package com.overcode250204.catalogservice.repository;

import com.overcode250204.catalogservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByTenantIdAndActive(UUID tenantId, boolean active, Pageable pageable);

    List<Product> findByTenantIdAndActive(UUID tenantId, boolean active);

    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndSku(UUID tenantId, String sku);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.categoryId = :categoryId AND p.active = true")
    Page<Product> findActiveByCategoryAndTenant(@Param("tenantId") UUID tenantId,
                                                @Param("categoryId") UUID categoryId,
                                                Pageable pageable);
}
