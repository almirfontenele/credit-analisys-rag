package com.credit.analysis.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessRuleDTO {
    @NotBlank private String ruleName;
    @NotBlank private String ruleDescription;
    @NotBlank private String ruleContent;
}
