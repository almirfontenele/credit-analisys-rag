package com.credit.analysis.integration;

import com.credit.analysis.domain.entity.BusinessRule;
import com.credit.analysis.domain.entity.Customer;
import com.credit.analysis.domain.entity.IncomeData;
import com.credit.analysis.domain.entity.CreditAnalysis;
import com.credit.analysis.domain.enums.CreditStatus;
import com.credit.analysis.repository.BusinessRuleRepository;
import com.credit.analysis.repository.CreditAnalysisRepository;
import com.credit.analysis.repository.CustomerRepository;
import com.credit.analysis.repository.IncomeDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class RepositoryIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BusinessRuleRepository businessRuleRepository;

    @Autowired
    private IncomeDataRepository incomeDataRepository;

    @Autowired
    private CreditAnalysisRepository creditAnalysisRepository;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = Customer.builder()
                .name("Carlos Teste Integrado")
                .cpf("77788899900")
                .email("carlos.teste@example.com")
                .phone("11987654321")
                .birthDate(LocalDate.of(1988, 5, 20))
                .address("Rua dos Testes, 100")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("CustomerRepository: Deve persistir e recuperar cliente por CPF ativo")
    void shouldPersistAndFindCustomerByCpfAndActiveTrue() {
        Customer saved = customerRepository.save(sampleCustomer);
        assertThat(saved.getId()).isNotNull();

        Optional<Customer> foundActive = customerRepository.findByCpfAndActiveTrue("77788899900");
        assertThat(foundActive).isPresent();
        assertThat(foundActive.get().getName()).isEqualTo("Carlos Teste Integrado");
        assertThat(foundActive.get().getActive()).isTrue();

        saved.setActive(false);
        customerRepository.save(saved);

        Optional<Customer> notFound = customerRepository.findByCpfAndActiveTrue("77788899900");
        assertThat(notFound).isEmpty();

        Optional<Customer> foundAny = customerRepository.findByCpf("77788899900");
        assertThat(foundAny).isPresent();
        assertThat(foundAny.get().getActive()).isFalse();
    }

    @Test
    @DisplayName("CustomerRepository: Deve validar existência por CPF")
    void shouldCheckExistenceByCpf() {
        customerRepository.save(sampleCustomer);

        assertThat(customerRepository.existsByCpf("77788899900")).isTrue();
        assertThat(customerRepository.existsByCpf("00011122233")).isFalse();
    }

    @Test
    @DisplayName("CreditAnalysisRepository: Deve persistir e recuperar análises por customerId")
    void shouldPersistAndRetrieveCreditAnalysisByCustomerId() {
        Customer savedCustomer = customerRepository.save(sampleCustomer);

        IncomeData incomeData = IncomeData.builder()
                .customer(savedCustomer)
                .monthlyIncome(new BigDecimal("5000.00"))
                .build();
        IncomeData savedIncome = incomeDataRepository.save(incomeData);

        CreditAnalysis analysis = CreditAnalysis.builder()
                .customer(savedCustomer)
                .incomeData(savedIncome)
                .status(CreditStatus.APPROVED)
                .requestedAmount(new BigDecimal("1500.00"))
                .declaredIncome(new BigDecimal("5000.00"))
                .approvedLimit(new BigDecimal("1500.00"))
                .analysisDetails("Aprovado com sucesso pelo teste de repositório")
                .ragContext("Regras vigentes")
                .aiResponse("STATUS: APPROVED")
                .build();

        CreditAnalysis savedAnalysis = creditAnalysisRepository.save(analysis);
        assertThat(savedAnalysis.getId()).isNotNull();

        List<CreditAnalysis> history = creditAnalysisRepository.findByCustomerId(savedCustomer.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus()).isEqualTo(CreditStatus.APPROVED);
        assertThat(history.get(0).getApprovedLimit()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("BusinessRuleRepository: Deve consultar findAll sem erro")
    void shouldFindAllBusinessRules() {
        List<BusinessRule> rules = businessRuleRepository.findAll();
        assertThat(rules).isNotEmpty();
    }

    @Test
    @DisplayName("BusinessRuleRepository: Deve persistir regras com vetores e consultar similaridade pgvector")
    void shouldPersistBusinessRuleAndQuerySimilarity() {
        float[] vector = new float[1536];
        vector[0] = 0.5f;
        vector[1] = 0.8f;

        BusinessRule rule = BusinessRule.builder()
                .ruleName("Regra de Limite Máximo Teste")
                .ruleDescription("Limite não pode exceder 33% da renda comprovada")
                .ruleContent("O limite máximo aprovado para crédito deve ser restrito a 33% do salário bruto mensal.")
                .embedding(vector)
                .build();

        BusinessRule saved = businessRuleRepository.save(rule);
        assertThat(saved.getId()).isNotNull();

        List<BusinessRule> similarRules = businessRuleRepository.findSimilarRules(vector, 3);
        assertThat(similarRules).isNotEmpty();
        assertThat(similarRules.stream().anyMatch(r -> r.getRuleName().equals("Regra de Limite Máximo Teste"))).isTrue();
    }
}
