package com.overcode250204.catalogservice.config.seeder;

import com.overcode250204.catalogservice.entity.Product;
import com.overcode250204.catalogservice.entity.ProductCategory;
import com.overcode250204.catalogservice.repository.ProductCategoryRepository;
import com.overcode250204.catalogservice.repository.ProductRepository;
import com.overcode250204.catalogservice.service.IProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogDataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final IProductSearchService productSearchService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Catalog Service database seeding...");

        UUID tenantId = UUID.fromString("d3b07384-d113-4c92-a1b6-d3aef6d9ef59");

        // 1. Seed Categories
        UUID electronicsId = UUID.fromString("ea89406e-8267-4e6f-8700-1cdc6b39d892");
        ProductCategory electronics = categoryRepository.findById(electronicsId)
                .orElseGet(() -> {
                    log.info("Seeding category Electronics");
                    return categoryRepository.save(ProductCategory.builder()
                            .id(electronicsId)
                            .tenantId(tenantId)
                            .name("Electronics")
                            .description("Electronics catalog items")
                            .active(true)
                            .createdAt(OffsetDateTime.now())
                            .build());
                });

        UUID officeSuppliesId = UUID.fromString("1dfbc370-dcc2-4df9-a1b7-d1a1b1a1b1a1");
        ProductCategory officeSupplies = categoryRepository.findById(officeSuppliesId)
                .orElseGet(() -> {
                    log.info("Seeding category Office Supplies");
                    return categoryRepository.save(ProductCategory.builder()
                            .id(officeSuppliesId)
                            .tenantId(tenantId)
                            .name("Office Supplies")
                            .description("Office supplies and productivity furniture")
                            .active(true)
                            .createdAt(OffsetDateTime.now())
                            .build());
                });

        // 2. Seed Products
        // Product 1: Mechanical Keyboard
        UUID kbId = UUID.fromString("5b2dbd80-b2dd-4e5c-a577-9be7bd39ca5a");
        if (!productRepository.existsById(kbId) && !productRepository.existsByTenantIdAndSku(tenantId, "ELE-KB-01")) {
            log.info("Seeding product Mechanical Keyboard");
            Product kb = productRepository.save(Product.builder()
                    .id(kbId)
                    .tenantId(tenantId)
                    .categoryId(electronics.getId())
                    .sku("ELE-KB-01")
                    .name("Mechanical Keyboard")
                    .description("Tactile mechanical keyboard, RGB backlit, blue switches")
                    .unit("pcs")
                    .price(new BigDecimal("1500000.0000"))
                    .currency("VND")
                    .active(true)
                    .createdAt(OffsetDateTime.now())
                    .build());
            productSearchService.sync(kb);
        }

        // Product 2: Wireless Mouse
        UUID msId = UUID.fromString("a9b2d880-a61c-4b5c-8977-9be7bd39ca5b");
        if (!productRepository.existsById(msId) && !productRepository.existsByTenantIdAndSku(tenantId, "ELE-MS-02")) {
            log.info("Seeding product Wireless Mouse");
            Product ms = productRepository.save(Product.builder()
                    .id(msId)
                    .tenantId(tenantId)
                    .categoryId(electronics.getId())
                    .sku("ELE-MS-02")
                    .name("Wireless Mouse")
                    .description("Ergonomic 2.4GHz wireless mouse with adjustable DPI")
                    .unit("pcs")
                    .price(new BigDecimal("500000.0000"))
                    .currency("VND")
                    .active(true)
                    .createdAt(OffsetDateTime.now())
                    .build());
            productSearchService.sync(ms);
        }

        // Product 3: Office Desk
        UUID dkId = UUID.fromString("9c26df80-a61c-4b5c-8977-9be7bd39ca5c");
        if (!productRepository.existsById(dkId) && !productRepository.existsByTenantIdAndSku(tenantId, "OFF-DK-03")) {
            log.info("Seeding product Office Desk");
            Product dk = productRepository.save(Product.builder()
                    .id(dkId)
                    .tenantId(tenantId)
                    .categoryId(officeSupplies.getId())
                    .sku("OFF-DK-03")
                    .name("Office Desk")
                    .description("Spacious wooden desk suitable for work and study")
                    .unit("pcs")
                    .price(new BigDecimal("2500000.0000"))
                    .currency("VND")
                    .active(true)
                    .createdAt(OffsetDateTime.now())
                    .build());
            productSearchService.sync(dk);
        }

        log.info("Catalog Service database seeding completed successfully.");
    }
}
