package com.overcode250204.catalogservice.config;

import com.overcode250204.catalogservice.entity.Product;
import com.overcode250204.catalogservice.repository.ProductRepository;
import com.overcode250204.catalogservice.service.IProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * ElasticsearchDataSeeder — runs at startup to bulk-sync all existing products
 * from PostgreSQL into the Elasticsearch "products" index.
 *
 * This resolves the empty-index problem: if the service restarts, ES has been
 * wiped, or new data was added while ES was down, this seeder re-indexes
 * everything so that /products/search returns real results immediately.
 *
 * Processing is done in pages of 100 to avoid loading all products into memory.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchDataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final IProductSearchService productSearchService;

    private static final int PAGE_SIZE = 100;

    @Override
    public void run(String... args) {
        log.info("[ElasticSeeder] Starting Elasticsearch product re-sync...");
        int page = 0;
        int totalSynced = 0;
        int totalFailed = 0;

        try {
            Page<Product> productPage;
            do {
                productPage = productRepository.findAll(PageRequest.of(page, PAGE_SIZE));
                for (Product product : productPage.getContent()) {
                    try {
                        productSearchService.sync(product);
                        totalSynced++;
                    } catch (Exception e) {
                        totalFailed++;
                        log.warn("[ElasticSeeder] Failed to sync product id={}: {}", product.getId(), e.getMessage());
                    }
                }
                page++;
            } while (productPage.hasNext());

            log.info("[ElasticSeeder] Completed. Synced={} Failed={}", totalSynced, totalFailed);
        } catch (Exception e) {
            // Do NOT block application startup if Elasticsearch is unavailable
            log.warn("[ElasticSeeder] Could not complete re-sync (ES may be starting up): {}", e.getMessage());
        }
    }
}
