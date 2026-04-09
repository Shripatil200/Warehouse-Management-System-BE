package com.infotact.warehouse.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Abstract base class for all persistence entities to provide automated auditing.
 * <p>
 * This class uses Spring Data JPA's {@link AuditingEntityListener} to automatically
 * manage creation and modification timestamps. By extending this class,
 * child entities inherit enterprise-standard tracking without manual boilerplate code.
 * </p>
 */
@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * The timestamp when the record was first persisted.
     * <p>
     * Logic: Set automatically upon insertion. This field is immutable
     * (updatable = false) to preserve the historical integrity of the record.
     * </p>
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * The timestamp of the most recent update to the record.
     * <p>
     * Logic: Updated automatically by Spring Data JPA whenever the entity
     * is modified and flushed to the database.
     * </p>
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}