package com.credit.analysis.integration;

import com.credit.analysis.domain.entity.BusinessRule;
import com.credit.analysis.domain.entity.CreditAnalysis;
import com.credit.analysis.domain.entity.Customer;
import com.credit.analysis.domain.enums.CreditStatus;
import com.credit.analysis.repository.BusinessRuleRepository;
import com.credit.analysis.repository.CreditAnalysisRepository;
import com.credit.analysis.repository.CustomerRepository;
import com.credit.analysis.repository.IncomeDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CreditAnalysisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BusinessRuleRepository businessRuleRepository;

    @Autowired
    private CreditAnalysisRepository creditAnalysisRepository;

    @Autowired
    private IncomeDataRepository incomeDataRepository;

    @MockBean
    private ChatClient.Builder chatClientBuilder;

    @MockBean
    private ChatClient chatClient;

    @MockBean
    private EmbeddingModel embeddingModel;

    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;

    private Customer activeCustomer;
    private Customer inactiveCustomer;

    @BeforeEach
    void setUp() {
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);

        float[] vector = new float[1536];
        when(embeddingModel.embed(anyString())).thenReturn(vector);

        // Cliente ativo para testes
        activeCustomer = customerRepository.save(Customer.builder()
                .name("Juliana Paes")
                .cpf("44455566677")
                .email("juliana.paes@test.com")
                .phone("11988884444")
                .birthDate(LocalDate.of(1989, 3, 12))
                .address("Rua Bela Cintra, 120")
                .active(true)
                .build());

        // Cliente inativo para testes
        inactiveCustomer = customerRepository.save(Customer.builder()
                .name("Marcos Inativo")
                .cpf("99988877711")
                .email("marcos.inativo@test.com")
                .phone("11977771111")
                .birthDate(LocalDate.of(1991, 7, 25))
                .address("Rua Consolação, 50")
                .active(false)
                .build());

        // Regra de negócio persistida
        businessRuleRepository.save(BusinessRule.builder()
                .ruleName("Regra Teto Salarial 33%")
                .ruleDescription("Teto máximo de 33% da renda")
                .ruleContent("o valor de solicitado de credito deve ser 0.33 do salario do customer")
                .embedding(vector)
                .build());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    @DisplayName("Fluxo Completo de Aprovação: Deve aprovar crédito compatível e persistir no banco")
    void shouldApproveCreditAndPersistInDatabase() throws Exception {
        String aiResponse = """
                STATUS: APPROVED
                LIMITE: 1000.00
                MOTIVACAO: O valor solicitado de R$ 1000.00 está dentro do teto de 33% do salário informado de R$ 3500.00.
                """;
        when(callSpec.content()).thenReturn(aiResponse);

        String json = """
                {
                    "cpf": "44455566677",
                    "salario": 3500.00,
                    "valor-credito-solicitado": 1000.00
                }
                """;

        mockMvc.perform(post("/api/credit-analysis/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedLimit").value(1000.00))
                .andExpect(jsonPath("$.customerName").value("Juliana Paes"))
                .andExpect(jsonPath("$.analysisId").isNotEmpty());

        // Validação da persistência real no PostgreSQL
        List<CreditAnalysis> history = creditAnalysisRepository.findByCustomerId(activeCustomer.getId());
        assertThat(history).isNotEmpty();

        CreditAnalysis saved = history.get(0);
        assertThat(saved.getStatus()).isEqualTo(CreditStatus.APPROVED);
        assertThat(saved.getRequestedAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(saved.getDeclaredIncome()).isEqualByComparingTo(new BigDecimal("3500.00"));
        assertThat(saved.getApprovedLimit()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(saved.getCustomer().getId()).isEqualTo(activeCustomer.getId());
        assertThat(saved.getIncomeData()).isNotNull();
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    @DisplayName("Fluxo Completo de Reprovação: Deve reprovar crédito incompatível e persistir REJECTED no banco")
    void shouldRejectCreditAndPersistInDatabase() throws Exception {
        String aiResponse = """
                STATUS: REJECTED
                LIMITE: 0.00
                MOTIVACAO: O valor solicitado de R$ 6000.00 excede severamente a margem de 33% da renda de R$ 2000.00.
                """;
        when(callSpec.content()).thenReturn(aiResponse);

        String json = """
                {
                    "cpf": "44455566677",
                    "salario": 2000.00,
                    "valor-credito-solicitado": 6000.00
                }
                """;

        mockMvc.perform(post("/api/credit-analysis/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.approvedLimit").value(0))
                .andExpect(jsonPath("$.analysisDetails").isNotEmpty());

        // Validação da persistência de REJECTED no PostgreSQL
        List<CreditAnalysis> history = creditAnalysisRepository.findByCustomerId(activeCustomer.getId());
        assertThat(history).isNotEmpty();

        CreditAnalysis saved = history.get(0);
        assertThat(saved.getStatus()).isEqualTo(CreditStatus.REJECTED);
        assertThat(saved.getRequestedAmount()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(saved.getDeclaredIncome()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(saved.getApprovedLimit()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    @DisplayName("Cliente Inexistente: Deve retornar 422 e não persistir nenhuma análise")
    void shouldReturn422WhenCustomerDoesNotExist() throws Exception {
        String json = """
                {
                    "cpf": "00000000000",
                    "salario": 4000.00,
                    "valor-credito-solicitado": 1000.00
                }
                """;

        mockMvc.perform(post("/api/credit-analysis/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Solicitante não possui cadastro no sistema. Regularize seu cadastro antes de solicitar crédito."));
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    @DisplayName("Cliente Inativo: Deve retornar 422 quando cadastro estiver inativo")
    void shouldReturn422WhenCustomerIsInactive() throws Exception {
        String json = """
                {
                    "cpf": "99988877711",
                    "salario": 5000.00,
                    "valor-credito-solicitado": 1000.00
                }
                """;

        mockMvc.perform(post("/api/credit-analysis/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Cadastro do cliente encontra-se inativo no sistema."));

        List<CreditAnalysis> history = creditAnalysisRepository.findByCustomerId(inactiveCustomer.getId());
        assertThat(history).isEmpty();
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    @DisplayName("GET /api/credit-analysis/history/{customerId}: Deve recuperar histórico de análises do cliente")
    void shouldRetrieveCustomerAnalysisHistory() throws Exception {
        // Criar registros de análise prévios no banco
        creditAnalysisRepository.save(CreditAnalysis.builder()
                .customer(activeCustomer)
                .status(CreditStatus.APPROVED)
                .requestedAmount(new BigDecimal("800.00"))
                .declaredIncome(new BigDecimal("3000.00"))
                .approvedLimit(new BigDecimal("800.00"))
                .analysisDetails("Primeira análise aprovada")
                .build());

        creditAnalysisRepository.save(CreditAnalysis.builder()
                .customer(activeCustomer)
                .status(CreditStatus.REJECTED)
                .requestedAmount(new BigDecimal("5000.00"))
                .declaredIncome(new BigDecimal("3000.00"))
                .approvedLimit(BigDecimal.ZERO)
                .analysisDetails("Segunda análise reprovada")
                .build());

        mockMvc.perform(get("/api/credit-analysis/history/" + activeCustomer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[1].status").value("REJECTED"));
    }
}
