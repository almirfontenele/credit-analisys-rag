package com.credit.analysis.controller;

import com.credit.analysis.domain.dto.CreditApplicationRequestDTO;
import com.credit.analysis.domain.dto.CreditApplicationResponseDTO;
import com.credit.analysis.domain.enums.CreditStatus;
import com.credit.analysis.exception.CreditIncompatibleException;
import com.credit.analysis.exception.CustomerInactiveException;
import com.credit.analysis.exception.CustomerNotFoundException;
import com.credit.analysis.service.RAGCreditAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CreditAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RAGCreditAnalysisService service;

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    @DisplayName("TASK-004/TASK-013: Deve permitir submissão de crédito por usuário com perfil CUSTOMER")
    void shouldAllowCreditSubmissionByCustomer() throws Exception {
        CreditApplicationResponseDTO response = CreditApplicationResponseDTO.builder()
                .analysisId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .customerName("Maria Silva")
                .cpf("12345678901")
                .salario(new BigDecimal("3500.00"))
                .valorCreditoSolicitado(new BigDecimal("1000.00"))
                .approvedLimit(new BigDecimal("1000.00"))
                .status(CreditStatus.APPROVED)
                .message("Crédito aprovado e cadastrado com sucesso")
                .analysisDetails("Aprovado conforme regras de negócio")
                .build();

        when(service.applyForCredit(any(CreditApplicationRequestDTO.class))).thenReturn(response);

        String json = """
                {
                    "cpf": "12345678901",
                    "salario": 3500.00,
                    "valor-credito-solicitado": 1000.00
                }
                """;

        mockMvc.perform(post("/api/credit-analysis/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.customerName").value("Maria Silva"))
                .andExpect(jsonPath("$.approvedLimit").value(1000.00));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("TASK-004/TASK-013: Deve permitir submissão de crédito por usuário com perfil ADMIN")
    void shouldAllowCreditSubmissionByAdmin() throws Exception {
        CreditApplicationResponseDTO response = CreditApplicationResponseDTO.builder()
                .analysisId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .customerName("João Santos")
                .cpf("98765432100")
                .salario(new BigDecimal("5000.00"))
                .valorCreditoSolicitado(new BigDecimal("1500.00"))
                .approvedLimit(new BigDecimal("1500.00"))
                .status(CreditStatus.APPROVED)
                .message("Crédito aprovado e cadastrado com sucesso")
                .build();

        when(service.applyForCredit(any(CreditApplicationRequestDTO.class))).thenReturn(response);

        String json = """
                {
                    "cpf": "98765432100",
                    "salario": 5000.00,
                    "valor-credito-solicitado": 1500.00
                }
                """;

        mockMvc.perform(post("/api/credit-analysis/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("TASK-013: Deve retornar 401 Unauthorized quando requisição for anônima")
    void shouldReturn401WhenAnonymous() throws Exception {
        String json = """
                {
                    "cpf": "12345678901",
                    "salario": 3500.00,
                    "valor-credito-solicitado": 1000.00
                }
                """;

        mockMvc.perform(post("/api/credit-analysis/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    @DisplayName("TASK-007: Deve retornar 422 Unprocessable Entity quando cliente não for encontrado")
    void shouldReturn422WhenCustomerNotFound() throws Exception {
        when(service.applyForCredit(any(CreditApplicationRequestDTO.class)))
                .thenThrow(new CustomerNotFoundException("Solicitante não possui cadastro no sistema."));

        String json = """
                {
                    "cpf": "00000000000",
                    "salario": 3000.00,
                    "valor-credito-solicitado": 500.00
                }
                """;

        mockMvc.perform(post("/api/credit-analysis/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Solicitante não possui cadastro no sistema."));
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    @DisplayName("TASK-007: Deve retornar 422 Unprocessable Entity quando cliente estiver inativo")
    void shouldReturn422WhenCustomerIsInactive() throws Exception {
        when(service.applyForCredit(any(CreditApplicationRequestDTO.class)))
                .thenThrow(new CustomerInactiveException("Cadastro do cliente encontra-se inativo no sistema."));

        String json = """
                {
                    "cpf": "11122233344",
                    "salario": 3000.00,
                    "valor-credito-solicitado": 500.00
                }
                """;

        mockMvc.perform(post("/api/credit-analysis/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Cadastro do cliente encontra-se inativo no sistema."));
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    @DisplayName("TASK-010: Deve retornar 200 OK com parecer detalhado quando crédito for REJECTED")
    void shouldReturn200WithRejectionDetailsWhenCreditIsRejected() throws Exception {
        CreditApplicationResponseDTO response = CreditApplicationResponseDTO.builder()
                .analysisId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .customerName("Carlos Andrade")
                .cpf("12345678901")
                .salario(new BigDecimal("1000.00"))
                .valorCreditoSolicitado(new BigDecimal("5000.00"))
                .approvedLimit(BigDecimal.ZERO)
                .status(CreditStatus.REJECTED)
                .message("Crédito reprovado de acordo com as políticas vigentes")
                .analysisDetails("Reprovado: O valor solicitado de R$ 5000.00 excede o teto permitido de 33% da renda.")
                .build();

        when(service.applyForCredit(any(CreditApplicationRequestDTO.class))).thenReturn(response);

        String json = """
                {
                    "cpf": "12345678901",
                    "salario": 1000.00,
                    "valor-credito-solicitado": 5000.00
                }
                """;

        mockMvc.perform(post("/api/credit-analysis/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.approvedLimit").value(0))
                .andExpect(jsonPath("$.analysisDetails").value("Reprovado: O valor solicitado de R$ 5000.00 excede o teto permitido de 33% da renda."));
    }

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("Compatibilidade: Deve retornar 402 quando CreditIncompatibleException for lançada")
    void shouldReturn402WhenCreditIsIncompatible() throws Exception {
        when(service.applyForCredit(any(CreditApplicationRequestDTO.class)))
                .thenThrow(new CreditIncompatibleException("valor solicitado incompativel com salario"));

        String json = """
                {
                    "cpf": "10861913825",
                    "salario": 1000.00,
                    "valor-credito-solicitado": 500.00
                }
                """;

        mockMvc.perform(post("/api/operators/credit-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is(402))
                .andExpect(jsonPath("$.status").value(402))
                .andExpect(jsonPath("$.message").value("valor solicitado incompativel com salario"));
    }
}
