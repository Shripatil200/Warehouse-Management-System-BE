package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistence entity representing a hierarchical classification for products.
 * <p>
 * This entity supports warehouse-level isolation and recursive sub-categorization.
 * It utilizes Hibernate Formulas to efficiently calculate product and
 * sub-category counts without triggering N+1 select issues.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@DynamicUpdate
@DynamicInsert
@Entity
@Table(
        name = "product_categories",
        indexes = {
                // Scoped index for uniqueness checks: name must be unique WITHIN a warehouse
                @Index(name = "idx_category_name_warehouse", columnList = "name, warehouse_id"),
                @Index(name = "idx_category_active", columnList = "active"),
                @Index(name = "idx_category_warehouse", columnList = "warehouse_id")
        }
)
public class ProductCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Link to the parent category in the hierarchy.
     * Root categories will have a null parent.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    @ToString.Exclude
    private ProductCategory parentCategory;

    /**
     * Strict multi-tenancy link.
     * Every category is owned by a specific warehouse.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    /**
     * UUID of the preferred zone (e.g., "COLD_STORAGE").
     * Used by the Putaway engine to suggest optimal bin placement.
     */
    @Column(name = "preferred_zone_id")
    private String preferredZoneId;

    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ProductCategory> subCategories = new ArrayList<>();

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Product> products = new ArrayList<>();

    // --- High-Performance Analytics (Read-Only) ---

    /**
     * Calculates the total number of active products in this specific category
     * using a database-level subquery.
     */
    @Formula("(SELECT COUNT(*) FROM products p WHERE p.category_id = id AND p.active = true)")
    private Integer productCount;

    /**
     * Calculates the number of direct children in the hierarchy.
     */
    @Formula("(SELECT COUNT(*) FROM product_categories pc WHERE pc.parent_category_id = id)")
    private Integer subCategoryCount;

    // --- Helper Methods ---

    /**
     * Adds a sub-category while maintaining the bidirectional relationship
     * and ensuring the child inherits the parent's warehouse.
     *
     * @param subCategory The nested category to add.
     */
    public void addSubCategory(ProductCategory subCategory) {
        if (subCategories == null) {
            subCategories = new ArrayList<>();
        }
        subCategories.add(subCategory);
        subCategory.setParentCategory(this);
        // Integrity Check: Sub-categories must belong to the same warehouse as the parent
        if (this.warehouse != null) {
            subCategory.setWarehouse(this.warehouse);
        }
    }

    /**
     * Utility to check if this category is a top-level root.
     */
    @Transient
    public boolean isRoot() {
        return this.parentCategory == null;
    }
}