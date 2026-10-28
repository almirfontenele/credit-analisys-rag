package com.credit.analysis.service;

import com.credit.analysis.domain.dto.CreditApplicationRequestDTO;
import com.credit.analysis.domain.dto.CreditApplicationResponseDTO;
import com.credit.analysis.domain.entity.BusinessRule;
import com.credit.analysis.domain.entity.CreditAnalysis;
import com.credit.analysis.domain.entity.Customer;
import com.credit.analysis.domain.entity.IncomeData;
import com.credit.analysis.domain.enums.CreditStatus;
import com.credit.analysis.exception.CustomerInactiveException;
import com.credit.analysis.exception.CustomerNotFoundException;
import com.credit.analysis.repository.CreditAnalysisRepository;
import com.credit.analysis.repository.CustomerRepository;
import com.credit.analysis.repository.IncomeDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RAGCreditAnalysisServiceTest {

    @Mock
    private CreditAnalysisRepository analysisRepository;

    @Mock
    private BusinessRuleService businessRuleService;

    @Mock
    private CustomerService customerService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private IncomeDataRepository incomeDataRepository;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    private RAGCreditAnalysisService service;

    private Customer sampleCustomer;
    private BusinessRule sampleRule;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        service = new RAGCreditAnalysisService(
                analysisRepository,
                businessRuleService,
                customerService,
                customerRepository,
                incomeDataRepository,
                chatClientBuilder
        );

        sampleCustomer = Customer.builder()
                .id(UUID.randomUUID())
                .name("Almir")
                .cpf("10861913825")
                .email("almirafo@gmail.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .active(true)
                .build();

        sampleRule = BusinessRule.builder()
                .id(UUID.randomUUID())
                .ruleName("dados de creditos valor")
                .ruleDescription("dados de creditos valor")
                .ruleContent("o valor de solicitado de credito deve ser 0.33 do salario do customer")
                .build();
    }

    @Test
    @DisplayName("TASK-007: Deve lançar CustomerNotFoundException quando CPF não possuir cadastro")
    void deveLancarExcecaoQuandoClienteNaoExistir() {
        when(customerRepository.findByCpf(anyString())).thenReturn(Optional.empty());

        CreditApplicationRequestDTO request = CreditApplicationRequestDTO.builder()
                .cpf("99999999999")
                .salario(new BigDecimal("3000.00"))
                .valorCreditoSolicitado(new BigDecimal("500.00"))
                .build();

        assertThrows(CustomerNotFoundException.class, () -> service.applyForCredit(request));
        verify(customerRepository, never()).save(any(Customer.class));
        verify(analysisRepository, never()).save(any(CreditAnalysis.class));
    }

    @Test
    @DisplayName("TASK-007: Deve lançar CustomerInactiveException quando cadastro do cliente estiver inativo")
    void deveLancarExcecaoQuandoClienteEstiverInativo() {
        sampleCustomer.setActive(false);
        when(customerRepository.findByCpf(anyString())).thenReturn(Optional.of(sampleCustomer));

        CreditApplicationRequestDTO request = CreditApplicationRequestDTO.builder()
                .cpf("10861913825")
                .salario(new BigDecimal("3000.00"))
                .valorCreditoSolicitado(new BigDecimal("500.00"))
                .build();

        assertThrows(CustomerInactiveException.class, () -> service.applyForCredit(request));
        verify(analysisRepository, never()).save(any(CreditAnalysis.class));
    }

    @Test
    @DisplayName("TASK-009: Deve persistir solicitação com status REJECTED e motivo quando incompatível")
    void deveGravarAnaliseComStatusRejectedEMotivoQuandoReprovado() {
        when(customerRepository.findByCpf(anyString())).thenReturn(Optional.of(sampleCustomer));
        when(businessRuleService.findSimilarRules(anyString(), anyInt())).thenReturn(List.of(sampleRule));
        when(incomeDataRepository.save(any(IncomeData.class))).thenAnswer(i -> i.getArgument(0));

        when(analysisRepository.save(any(CreditAnalysis.class))).thenAnswer(invocation -> {
            CreditAnalysis ca = invocation.getArgument(0);
            ca.setId(UUID.randomUUID());
            return ca;
        });

        CreditApplicationRequestDTO request = CreditApplicationRequestDTO.builder()
                .cpf("10861913825")
                .salario(new BigDecimal("1000.00"))
                .valorCreditoSolicitado(new BigDecimal("500.00")) // Excede 33% (330.00)
                .build();

        CreditApplicationResponseDTO response = service.applyForCredit(request);

        assertNotNull(response);
        assertEquals(CreditStatus.REJECTED, response.getStatus());
        assertEquals(BigDecimal.ZERO, response.getApprovedLimit());
        assertNotNull(response.getAnalysisDetails());
        assertTrue(response.getAnalysisDetails().contains("Reprovado"));

        ArgumentCaptor<CreditAnalysis> captor = ArgumentCaptor.forClass(CreditAnalysis.class);
        verify(analysisRepository, times(1)).save(captor.capture());
        CreditAnalysis saved = captor.getValue();
        assertEquals(CreditStatus.REJECTED, saved.getStatus());
        assertEquals(BigDecimal.ZERO, saved.getApprovedLimit());
        assertNotNull(saved.getAnalysisDetails());
    }

    @Test
    @DisplayName("TASK-009/010: Deve aprovar e persistir com status APPROVED quando compatível")
    void deveGravarAnaliseComStatusApprovedQuandoCompativel() {
        when(customerRepository.findByCpf(anyString())).thenReturn(Optional.of(sampleCustomer));
        when(businessRuleService.findSimilarRules(anyString(), anyInt())).thenReturn(List.of(sampleRule));
        when(incomeDataRepository.save(any(IncomeData.class))).thenAnswer(i -> i.getArgument(0));

        when(analysisRepository.save(any(CreditAnalysis.class))).thenAnswer(invocation -> {
            CreditAnalysis ca = invocation.getArgument(0);
            ca.setId(UUID.randomUUID());
            return ca;
        });

        CreditApplicationRequestDTO request = CreditApplicationRequestDTO.builder()
                .cpf("10861913825")
                .salario(new BigDecimal("10000.00"))
                .valorCreditoSolicitado(new BigDecimal("3000.00")) // Dentro dos 33% (3300.00)
                .build();

        CreditApplicationResponseDTO response = service.applyForCredit(request);

        assertNotNull(response);
        assertEquals(CreditStatus.APPROVED, response.getStatus());
        assertEquals(new BigDecimal("3000.00"), response.getValorCreditoSolicitado());
        assertEquals(new BigDecimal("3000.00"), response.getApprovedLimit());
        assertEquals("10861913825", response.getCpf());

        ArgumentCaptor<CreditAnalysis> captor = ArgumentCaptor.forClass(CreditAnalysis.class);
        verify(analysisRepository, times(1)).save(captor.capture());
        assertEquals(CreditStatus.APPROVED, captor.getValue().getStatus());
    }
}
