package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import com.infotact.warehouse.entity.enums.BinRentalPaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a single billing period for a {@link BinRental}.
 * <p>
 * Generated periodically by the rental billing scheduler (or manually by a MANAGER).
 * totalAmount = binRental.rentalRatePerDay × totalDays.
 * </p>
 */
@Getter
@Setter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "bin_rental_payments",
        indexes = {
                @Index(name = "idx_bin_rental_payment_rental", columnList = "bin_rental_id"),
                @Index(name = "idx_bin_rental_payment_status", columnList = "status"),
                @Index(name = "idx_bin_rental_payment_period", columnList = "period_from, period_to")
        }
)
public class BinRentalPayment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * The rental agreement this payment belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bin_rental_id", nullable = false)
    private BinRental binRental;

    /**
     * Start of the billing window (inclusive).
     */
    @Column(nullable = false)
    private LocalDate periodFrom;

    /**
     * End of the billing window (inclusive).
     */
    @Column(nullable = false)
    private LocalDate periodTo;

    /**
     * Number of calendar days covered: periodTo − periodFrom + 1.
     */
    @Column(nullable = false)
    private Integer totalDays;

    /**
     * Total amount due: rentalRatePerDay × totalDays.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    /**
     * Payment lifecycle state (PENDING → PAID).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BinRentalPaymentStatus status = BinRentalPaymentStatus.PENDING;

    /**
     * Timestamp when the supplier remitted this payment. Null until PAID.
     */
    private LocalDateTime paidAt;
}
