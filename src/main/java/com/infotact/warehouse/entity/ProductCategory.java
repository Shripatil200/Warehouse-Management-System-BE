package com.infotact.warehouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "product_categories")
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private ProductCategory parentCategory;

    // This allows us to check if a category has children before deleting
    @OneToMany(mappedBy = "parentCategory")
    private List<ProductCategory> subCategories;

    @OneToMany(mappedBy = "category")
    private List<Product> products;

    public ProductCategory(String name, ProductCategory parentCategory) {
        this.name = name;
        this.parentCategory = parentCategory;
    }
}