package com.credit.analysis.domain.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerDTO {
    @NotBlank private String name;
    @NotBlank @Size(min = 11, max = 11) private String cpf;
    @NotBlank @Email private String email;
    private String phone;
    @NotNull private LocalDate birthDate;
    private String address;
    private Boolean active;
}
