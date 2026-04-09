package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistence entity representing a hierarchical classification for products.
 * <p>
 * This entity supports a recursive tree structure (parent-child) and includes
 * operational metadata like 'preferredZoneId' to assist in automated
 * put-away logic.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@DynamicUpdate
@DynamicInsert
@Entity
@Table(name = "product_categories", indexes = {
        @Index(name = "idx_category_name", columnList = "name"),
        @Index(name = "idx_category_warehouse", columnList = "warehouse_id")
})
public class ProductCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String name;

    /**
     * Operational status for the category.
     * <p>
     * Logic: Inactive categories hide all associated products from the
     * main browsing view, effectively acting as a bulk soft-delete.
     * </p>
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Self-referencing link to the parent classification.
     * <p>
     * Null indicates this is a top-level (root) category.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private ProductCategory parentCategory;

    /**
     * DENORMALIZATION: Direct link to Warehouse.
     * <p>
     * Isolation: Ensures that category trees are facility-specific, preventing
     * a manager in Warehouse A from seeing the custom category logic of Warehouse B.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    /**
     * Optimization Hint: The UUID of the zone where products of this
     * category are ideally stored (e.g., 'Cold Storage' for Perishables).
     */
    @Column(name = "preferred_zone_id")
    private String preferredZoneId;

    /**
     * List of nested sub-categories.
     */
    @OneToMany(mappedBy = "parentCategory")
    private List<ProductCategory> subCategories = new ArrayList<>();

    /**
     * List of products belonging directly to this category level.
     */
    @OneToMany(mappedBy = "category")
    private List<Product> products = new ArrayList<>();
}