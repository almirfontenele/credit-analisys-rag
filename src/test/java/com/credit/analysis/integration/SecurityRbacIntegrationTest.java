package com.credit.analysis.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ==========================================
    // 1. ROTAS PÚBLICAS (Swagger / OpenAPI)
    // ==========================================

    @Test
    @DisplayName("Público: Deve permitir acesso anônimo ao Swagger e Documentação da API")
    void shouldAllowAnonymousAccessToSwaggerAndApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 2. ROTAS ADMINISTRATIVAS (/api/admin/**)
    // ==========================================

    @Test
    @DisplayName("Admin: Deve retornar 401 quando requisição anônima tentar acessar rota administrativa")
    void shouldReturn401WhenAnonymousAccessAdminRoute() throws Exception {
        mockMvc.perform(get("/api/admin/business-rules"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    @DisplayName("Admin: Deve retornar 403 quando perfil CUSTOMER tentar acessar rota administrativa")
    void shouldReturn403WhenCustomerAccessAdminRoute() throws Exception {
        mockMvc.perform(get("/api/admin/business-rules"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("Admin: Deve retornar 403 quando perfil OPERATOR tentar acessar rota administrativa")
    void shouldReturn403WhenOperatorAccessAdminRoute() throws Exception {
        mockMvc.perform(get("/api/admin/business-rules"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin: Deve permitir acesso quando perfil for ADMIN")
    void shouldAllowAdminAccessAdminRoute() throws Exception {
        mockMvc.perform(get("/api/admin/business-rules"))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 3. ROTAS DE OPERADORES (/api/operators/**)
    // ==========================================

    @Test
    @DisplayName("Operador: Deve retornar 401 para anônimo em rota de operadores")
    void shouldReturn401WhenAnonymousAccessOperatorRoute() throws Exception {
        mockMvc.perform(get("/api/operators/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    @DisplayName("Operador: Deve retornar 403 para CUSTOMER em rota de operadores")
    void shouldReturn403WhenCustomerAccessOperatorRoute() throws Exception {
        mockMvc.perform(get("/api/operators/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("Operador: Deve permitir acesso para perfil OPERATOR")
    void shouldAllowOperatorAccessOperatorRoute() throws Exception {
        mockMvc.perform(get("/api/operators/customers"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Operador: Deve permitir acesso de ADMIN em rota de operadores")
    void shouldAllowAdminAccessOperatorRoute() throws Exception {
        mockMvc.perform(get("/api/operators/customers"))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 4. ROTAS DE PERFIL DO CLIENTE (/api/customers/me/**)
    // ==========================================

    @Test
    @DisplayName("Customer Me: Deve retornar 401 para anônimo")
    void shouldReturn401WhenAnonymousAccessCustomerMe() throws Exception {
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("Customer Me: Deve retornar 403 para OPERATOR")
    void shouldReturn403WhenOperatorAccessCustomerMe() throws Exception {
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Customer Me: Deve retornar 403 para ADMIN")
    void shouldReturn403WhenAdminAccessCustomerMe() throws Exception {
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 5. ROTAS DE ANÁLISE DE CRÉDITO (/api/credit-analysis/**)
    // ==========================================

    @Test
    @DisplayName("Credit Analysis: Deve retornar 401 para anônimo")
    void shouldReturn401WhenAnonymousAccessCreditAnalysis() throws Exception {
        mockMvc.perform(get("/api/credit-analysis/history/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    @DisplayName("Credit Analysis: Deve permitir acesso de consulta de histórico para CUSTOMER")
    void shouldAllowCustomerAccessCreditHistory() throws Exception {
        mockMvc.perform(get("/api/credit-analysis/history/" + UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    @DisplayName("Credit Analysis: Deve permitir acesso de consulta de histórico para OPERATOR")
    void shouldAllowOperatorAccessCreditHistory() throws Exception {
        mockMvc.perform(get("/api/credit-analysis/history/" + UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Credit Analysis: Deve permitir acesso de consulta de histórico para ADMIN")
    void shouldAllowAdminAccessCreditHistory() throws Exception {
        mockMvc.perform(get("/api/credit-analysis/history/" + UUID.randomUUID()))
                .andExpect(status().isOk());
    }
}
