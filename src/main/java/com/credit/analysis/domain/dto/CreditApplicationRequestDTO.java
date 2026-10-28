package com.credit.analysis.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditApplicationRequestDTO {

    @NotBlank(message = "CPF é obrigatório")
    private String cpf;

    @NotNull(message = "Salário é obrigatório")
    @Positive(message = "Salário deve ser maior que zero")
    private BigDecimal salario;

    @NotNull(message = "Valor de crédito solicitado é obrigatório")
    @Positive(message = "Valor de crédito solicitado deve ser maior que zero")
    @JsonProperty("valor-credito-solicitado")
    @JsonAlias({"valorCreditoSolicitado", "valor_credito_solicitado"})
    private BigDecimal valorCreditoSolicitado;
}
