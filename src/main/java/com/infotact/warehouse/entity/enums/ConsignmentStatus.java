package com.infotact.warehouse.entity.enums;

/**
 * Lifecycle states for a supplier consignment agreement.
 *
 * Flow:
 *   PENDING_APPROVAL → ACTIVE → SETTLED (per settlement cycle)
 *   ACTIVE → TERMINATED (by either party)
 *   PENDING_APPROVAL → REJECTED
 */
public enum ConsignmentStatus {

    /**
     * Supplier submitted the consignment request; awaiting warehouse MANAGER approval.
     */
    PENDING_APPROVAL,

    /**
     * Approved and active. Products under this agreement are stored and sold on
     * behalf of the supplier. Settlement runs on the configured cycle.
     */
    ACTIVE,

    /**
     * A settlement cycle has been completed and profit has been disbursed.
     * A new cycle immediately begins (status goes back to ACTIVE on next cycle start).
     * This status marks the *settled snapshot* record, not the agreement itself.
     */
    SETTLED,

    /**
     * Agreement was rejected by the warehouse manager during review.
     */
    REJECTED,

    /**
     * Agreement was terminated (either party). No further selling activity allowed.
     */
    TERMINATED
}
