package com.credit.analysis.domain.dto;

import com.credit.analysis.domain.enums.CreditStatus;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditAnalysisResponse {
    private UUID analysisId;
    private UUID customerId;
    private String customerName;
    private CreditStatus status;
    private BigDecimal approvedLimit;
    private String analysisDetails;
    private String aiResponse;
}
