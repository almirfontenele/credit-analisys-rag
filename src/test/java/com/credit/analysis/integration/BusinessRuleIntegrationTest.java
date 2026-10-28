package com.credit.analysis.integration;

import com.credit.analysis.domain.entity.BusinessRule;
import com.credit.analysis.repository.BusinessRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class BusinessRuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BusinessRuleRepository businessRuleRepository;

    @MockBean
    private EmbeddingModel embeddingModel;

    private float[] dummyVector;

    @BeforeEach
    void setUp() {
        dummyVector = new float[1536];
        dummyVector[0] = 0.25f;
        dummyVector[1] = 0.75f;
        when(embeddingModel.embed(anyString())).thenReturn(dummyVector);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/admin/business-rules: Deve criar regra e persistir vetor no banco")
    void shouldCreateBusinessRuleAndPersistInDatabase() throws Exception {
        String json = """
                {
                    "ruleName": "Regra Limite 30 Porcento",
                    "ruleDescription": "Compromete no máximo 30% da renda",
                    "ruleContent": "O crédito concedido não deve ultrapassar 30% da renda comprovada."
                }
                """;

        mockMvc.perform(post("/api/admin/business-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.ruleName").value("Regra Limite 30 Porcento"))
                .andExpect(jsonPath("$.ruleDescription").value("Compromete no máximo 30% da renda"));

        Optional<BusinessRule> found = businessRuleRepository.findAll().stream()
                .filter(r -> "Regra Limite 30 Porcento".equals(r.getRuleName()))
                .findFirst();

        assertThat(found).isPresent();
        assertThat(found.get().getEmbedding()).isNotNull();
        assertThat(found.get().getEmbedding()[0]).isEqualTo(0.25f);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/business-rules/{id}: Deve consultar regra cadastrada")
    void shouldRetrieveBusinessRuleById() throws Exception {
        BusinessRule rule = BusinessRule.builder()
                .ruleName("Regra Score Mínimo")
                .ruleDescription("Score de crédito mínimo aceitável")
                .ruleContent("Clientes com score abaixo de 400 não são elegíveis a crédito pré-aprovado.")
                .embedding(dummyVector)
                .build();
        BusinessRule saved = businessRuleRepository.save(rule);

        mockMvc.perform(get("/api/admin/business-rules/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.ruleName").value("Regra Score Mínimo"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/business-rules/search: Deve buscar regras similares via pgvector")
    void shouldSearchSimilarRulesUsingVectorStore() throws Exception {
        BusinessRule rule = BusinessRule.builder()
                .ruleName("Regra Busca Similaridade")
                .ruleDescription("Similaridade pgvector")
                .ruleContent("Regra de teste para busca vetorial de similaridade.")
                .embedding(dummyVector)
                .build();
        businessRuleRepository.save(rule);

        mockMvc.perform(get("/api/admin/business-rules/search")
                        .param("query", "regra similaridade")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/admin/business-rules/{id}: Deve atualizar regra e seu embedding")
    void shouldUpdateBusinessRule() throws Exception {
        BusinessRule rule = BusinessRule.builder()
                .ruleName("Regra Original")
                .ruleDescription("Descrição antiga")
                .ruleContent("Conteúdo antigo")
                .embedding(dummyVector)
                .build();
        BusinessRule saved = businessRuleRepository.save(rule);

        String updateJson = """
                {
                    "ruleName": "Regra Atualizada",
                    "ruleDescription": "Descrição nova",
                    "ruleContent": "Conteúdo novo e revisado"
                }
                """;

        mockMvc.perform(put("/api/admin/business-rules/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleName").value("Regra Atualizada"))
                .andExpect(jsonPath("$.ruleDescription").value("Descrição nova"));

        BusinessRule updated = businessRuleRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getRuleName()).isEqualTo("Regra Atualizada");
        assertThat(updated.getRuleContent()).isEqualTo("Conteúdo novo e revisado");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("DELETE /api/admin/business-rules/{id}: Deve deletar regra do banco")
    void shouldDeleteBusinessRule() throws Exception {
        BusinessRule rule = BusinessRule.builder()
                .ruleName("Regra Para Deletar")
                .ruleDescription("Desc")
                .ruleContent("Content")
                .embedding(dummyVector)
                .build();
        BusinessRule saved = businessRuleRepository.save(rule);

        mockMvc.perform(delete("/api/admin/business-rules/" + saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(businessRuleRepository.findById(saved.getId())).isEmpty();
    }
}
