package com.overcode250204.catalogservice.service;

import com.overcode250204.catalogservice.dto.CreateProductRequest;
import com.overcode250204.catalogservice.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * IProductService — Service interface defining business contracts for catalog
 * and product management.
 */
public interface IProductService {

    /**
     * Retrieves a flat list of catalog products for a given tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return a list containing matching product response objects
     */
    List<ProductResponse> getProducts(UUID tenantId);

    /**
     * Looks up details of a specific product by its ID and tenant ID.
     *
     * @param tenantId the unique identifier of the tenant owning the product
     * @param id       the unique identifier of the product
     * @return the details of the product
     * @throws com.overcode250204.catalogservice.exception.ProductNotFoundException if
     *                                                                              the
     *                                                                              product
     *                                                                              does
     *                                                                              not
     *                                                                              exist
     */
    ProductResponse getProductById(UUID tenantId, UUID id);

    /**
     * Creates a new product in the catalog.
     *
     * @param tenantId the unique identifier of the tenant owning the product
     * @param request  the parameters to build the new product
     * @return the details of the newly created product
     * @throws com.overcode250204.catalogservice.exception.SkuAlreadyExistsException if
     *                                                                               a
     *                                                                               product
     *                                                                               with
     *                                                                               the
     *                                                                               same
     *                                                                               SKU
     *                                                                               already
     *                                                                               exists
     */
    ProductResponse createProduct(UUID tenantId, CreateProductRequest request);

    /**
     * Deactivates a product, changing its status and evicting it from any local
     * caches.
     *
     * @param tenantId the unique identifier of the tenant
     * @param id       the unique identifier of the product to deactivate
     * @throws com.overcode250204.catalogservice.exception.ProductNotFoundException if
     *                                                                              the
     *                                                                              product
     *                                                                              does
     *                                                                              not
     *                                                                              exist
     */
    void deactivateProduct(UUID tenantId, UUID id);

    /**
     * Updates the status of a specific product.
     *
     * @param tenantId the unique identifier of the tenant owning the product
     * @param id       the unique identifier of the product
     * @param status   the status to apply (active / inactive / draft)
     * @return the details of the updated product
     * @throws com.overcode250204.catalogservice.exception.ProductNotFoundException if
     *                                                                              the
     *                                                                              product
     *                                                                              does
     *                                                                              not
     *                                                                              exist
     */
    ProductResponse updateStatus(UUID tenantId, UUID id, String status);

    /**
     * Validates and computes the total price for a specified quantity of a product.
     *
     * @param tenantId the unique identifier of the tenant
     * @param id       the unique identifier of the product
     * @param qty      the quantity to compute
     * @return a map containing price breakdown details (base, discount, total)
     * @throws com.overcode250204.catalogservice.exception.ProductNotFoundException if
     *                                                                              the
     *                                                                              product
     *                                                                              does
     *                                                                              not
     *                                                                              exist
     */
    Map<String, Object> calculatePrice(UUID tenantId, UUID id, int qty);

    /**
     * Performs fuzzy autocompletion searching on active product titles.
     *
     * @param q the query search string
     * @return list of autocompleted terms matching the prefix
     */
    List<String> autocomplete(String q);

    /**
     * Retrieves catalog recommendation links related to a specific product.
     *
     * @param id the unique identifier of the pivot product
     * @return list of similar or matching replacement products
     */
    List<Map<String, Object>> getRecommendations(UUID id);

    /**
     * Gathers analytics statistics on search terms typed by users.
     *
     * @return list of maps conveying search query statistics
     */
    List<Map<String, Object>> getSearchAnalytics();

    /**
     * Triggers Elasticsearch or search engine re-indexing on a given entity
     * category.
     *
     * @param type the catalog type to re-index
     * @return status map indicating process receipt
     */
    Map<String, Object> triggerReindex(String type);
}
