package com.credit.analysis.domain.dto;

import com.credit.analysis.domain.enums.CreditStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditApplicationResponseDTO {
    private UUID analysisId;
    private UUID customerId;
    private String customerName;
    private String cpf;
    private BigDecimal salario;
    private BigDecimal valorCreditoSolicitado;
    private BigDecimal approvedLimit;
    private CreditStatus status;
    private String message;
    private String analysisDetails;
}
