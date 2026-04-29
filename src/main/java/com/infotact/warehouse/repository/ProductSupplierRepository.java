package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.ProductSupplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link ProductSupplier} instances.
 * <p>
 * Provides specialized query methods for sourcing analytics. This allows the
 * system to retrieve all available vendors for a specific item, enabling
 * cost-comparison logic for warehouse managers.
 * </p>
 */
@Repository
public interface ProductSupplierRepository extends JpaRepository<ProductSupplier, String> {

    /**
     * Retrieves all sourcing options associated with a specific product.
     * <p>
     * <b>Use Case:</b> Populating the 'sourcingOptions' list in the ProductResponse DTO.
     * </p>
     * @param productId The UUID of the product.
     * @return A list of ProductSupplier entities containing vendor pricing and lead times.
     */
    List<ProductSupplier> findByProductId(String productId);

    /**
     * Retrieves a specific sourcing link between a product and a supplier.
     * @param productId The UUID of the product.
     * @param supplierId The UUID of the supplier.
     * @return An Optional containing the sourcing link if it exists.
     */
    Optional<ProductSupplier> findByProductIdAndSupplierId(String productId, String supplierId);

    /**
     * Finds the most cost-effective supplier for a specific product.
     * <p>
     * <b>Logic:</b> Returns the sourcing record with the lowest <b>currentSupplyPrice</b>.
     * </p>
     * @param productId The UUID of the product.
     * @return The cheapest sourcing option available.
     */
    @Query("SELECT ps FROM ProductSupplier ps WHERE ps.product.id = :productId ORDER BY ps.currentSupplyPrice ASC LIMIT 1")
    Optional<ProductSupplier> findCheapestSupplier(@Param("productId") String productId);

    /**
     * Checks if a specific supplier is already linked to a product.
     * @param productId The UUID of the product.
     * @param supplierId The UUID of the supplier.
     * @return True if the relationship exists.
     */
    boolean existsByProductIdAndSupplierId(String productId, String supplierId);
}