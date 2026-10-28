package com.credit.analysis.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncomeDataDTO {
    @NotNull private UUID customerId;
    @NotNull private BigDecimal monthlyIncome;
    private String employmentType;
    private String employerName;
    private Integer employmentTimeYears;
    private BigDecimal otherIncome;
    private BigDecimal existingDebts;
    private Integer creditScore;
}
