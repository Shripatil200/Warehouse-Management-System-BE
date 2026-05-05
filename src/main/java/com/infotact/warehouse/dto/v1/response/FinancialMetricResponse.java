package com.infotact.warehouse.dto.v1.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor // Required for some mapping operations
@AllArgsConstructor // This is what JPQL 'new' uses
public class FinancialMetricResponse {
    private String period;

    // Changing these to Double often fixes validation failures with SUM()
    // unless you explicitly cast in the query.
    private Double inboundValue;
    private Double outboundValue;
    private Double adjustmentLoss;
    private Double grossMargin;
}