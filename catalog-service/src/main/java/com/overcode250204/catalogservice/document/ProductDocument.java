package com.overcode250204.catalogservice.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * ProductDocument — Elasticsearch index document.
 * Index: "products"
 *
 * Enables full-text search on product name, description, sku.
 * Synced from catalog-service whenever a product is created or updated.
 */
@Document(indexName = "products", createIndex = true)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name = "tenant_id")
    private String tenantId;

    @Field(type = FieldType.Keyword, name = "category_id")
    private String categoryId;

    @Field(type = FieldType.Keyword)
    private String sku;

    /**
     * Analyzed field — supports partial keyword match "MacBook Pro" → "macbook", "pro"
     */
    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "standard"), otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
    })
    private String name;

    /**
     * Full-text description — analyzed for content-level product search.
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String unit;

    /**
     * basePrice — mirrors the product's default price; named to match FE Product type.
     */
    @Field(type = FieldType.Double, name = "base_price")
    private BigDecimal basePrice;

    @Field(type = FieldType.Keyword)
    private String currency;

    @Field(type = FieldType.Boolean, name = "is_active")
    private boolean active;

    /**
     * status — derived from active flag: "active" | "inactive"
     * Matches the FE ProductStatus type so the FE can render status badges directly.
     */
    @Field(type = FieldType.Keyword)
    private String status;

    /**
     * pricingTiers — embedded as nested objects so the FE ProductDrawer can
     * display tiered pricing without a second REST call.
     */
    @Field(type = FieldType.Nested)
    private List<PricingTierDocument> pricingTiers;

    @Field(type = FieldType.Date, name = "created_at", format = DateFormat.date_optional_time)
    private OffsetDateTime createdAt;

    @Field(type = FieldType.Date, name = "updated_at", format = DateFormat.date_optional_time)
    private OffsetDateTime updatedAt;

    /**
     * Embedded pricing tier for nested Elasticsearch mapping.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingTierDocument {

        @Field(type = FieldType.Keyword)
        private String id;

        @Field(type = FieldType.Integer, name = "min_qty")
        private int minQty;

        @Field(type = FieldType.Double, name = "unit_price")
        private BigDecimal unitPrice;

        @Field(type = FieldType.Keyword)
        private String label;
    }
}
