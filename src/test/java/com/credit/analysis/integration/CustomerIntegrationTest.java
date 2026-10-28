package com.credit.analysis.integration;

import com.credit.analysis.domain.entity.Customer;
import com.credit.analysis.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CustomerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .name("Mariana Oliveira")
                .cpf("11122233399")
                .email("mariana.oliveira@test.com")
                .phone("1199998888")
                .birthDate(LocalDate.of(1992, 4, 15))
                .address("Av. Paulista, 1000")
                .active(true)
                .build();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/customers: Deve criar novo cliente e persistir no banco de dados")
    void shouldCreateCustomerAndPersistInDatabase() throws Exception {
        String json = """
                {
                    "name": "Roberto Dias",
                    "cpf": "22233344455",
                    "email": "roberto.dias@test.com",
                    "phone": "11988887777",
                    "birthDate": "1985-10-20",
                    "address": "Rua Augusta, 500",
                    "active": true
                }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Roberto Dias"))
                .andExpect(jsonPath("$.cpf").value("22233344455"))
                .andExpect(jsonPath("$.active").value(true));

        Optional<Customer> persisted = customerRepository.findByCpf("22233344455");
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getName()).isEqualTo("Roberto Dias");
        assertThat(persisted.get().getEmail()).isEqualTo("roberto.dias@test.com");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/customers: Deve rejeitar criação quando CPF já estiver cadastrado")
    void shouldRejectCreationWithDuplicateCpf() throws Exception {
        customerRepository.save(testCustomer);

        String duplicateJson = """
                {
                    "name": "Mariana Clonada",
                    "cpf": "11122233399",
                    "email": "mariana.clone@test.com",
                    "birthDate": "1992-04-15"
                }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CPF já cadastrado"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/customers: Deve validar dados obrigatórios (@Valid) e retornar 400")
    void shouldValidateCustomerDtoFields() throws Exception {
        String invalidJson = """
                {
                    "name": "",
                    "cpf": "123",
                    "email": "not-an-email"
                }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isMap());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /api/customers/{id} e /cpf/{cpf}: Deve recuperar cliente existente")
    void shouldRetrieveCustomerByIdAndCpf() throws Exception {
        Customer saved = customerRepository.save(testCustomer);

        // Por ID
        mockMvc.perform(get("/api/customers/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.cpf").value("11122233399"))
                .andExpect(jsonPath("$.name").value("Mariana Oliveira"));

        // Por CPF
        mockMvc.perform(get("/api/customers/cpf/" + saved.getCpf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.email").value("mariana.oliveira@test.com"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /api/customers/{id}: Deve retornar 422 quando ID não existir")
    void shouldReturn422WhenCustomerIdNotFound() throws Exception {
        mockMvc.perform(get("/api/customers/" + UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Cliente não encontrado"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/customers/{id}: Deve atualizar dados cadastrais do cliente")
    void shouldUpdateCustomer() throws Exception {
        Customer saved = customerRepository.save(testCustomer);

        String updateJson = """
                {
                    "name": "Mariana Oliveira Santos",
                    "cpf": "11122233399",
                    "email": "mariana.santos@test.com",
                    "phone": "11911112222",
                    "birthDate": "1992-04-15",
                    "address": "Av. Brigadeiro Faria Lima, 2000",
                    "active": true
                }
                """;

        mockMvc.perform(put("/api/customers/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mariana Oliveira Santos"))
                .andExpect(jsonPath("$.email").value("mariana.santos@test.com"));

        Customer updated = customerRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Mariana Oliveira Santos");
        assertThat(updated.getAddress()).isEqualTo("Av. Brigadeiro Faria Lima, 2000");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/admin/customers/{id}/status: Deve alterar status ativo/inativo do cliente")
    void shouldUpdateCustomerStatusByAdmin() throws Exception {
        Customer saved = customerRepository.save(testCustomer);
        assertThat(saved.getActive()).isTrue();

        // Inativar
        mockMvc.perform(put("/api/admin/customers/" + saved.getId() + "/status")
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        Customer inactived = customerRepository.findById(saved.getId()).orElseThrow();
        assertThat(inactived.getActive()).isFalse();

        // Reativar
        mockMvc.perform(put("/api/admin/customers/" + saved.getId() + "/status")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        Customer reactivated = customerRepository.findById(saved.getId()).orElseThrow();
        assertThat(reactivated.getActive()).isTrue();
    }

    @Test
    @WithMockUser(username = "11122233399", roles = {"CUSTOMER"})
    @DisplayName("GET & PUT /api/customers/me: Cliente autenticado consulta e atualiza seu perfil")
    void shouldGetAndUpdateCustomerMeProfile() throws Exception {
        customerRepository.save(testCustomer);

        // GET /me
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("11122233399"))
                .andExpect(jsonPath("$.name").value("Mariana Oliveira"));

        // PUT /me
        String updateMeJson = """
                {
                    "name": "Mariana O. Silva",
                    "email": "mariana.nova@test.com",
                    "phone": "11988880000",
                    "address": "Rua Oscar Freire, 300"
                }
                """;

        mockMvc.perform(put("/api/customers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateMeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mariana O. Silva"))
                .andExpect(jsonPath("$.email").value("mariana.nova@test.com"));

        Customer updatedInDb = customerRepository.findByCpf("11122233399").orElseThrow();
        assertThat(updatedInDb.getName()).isEqualTo("Mariana O. Silva");
        assertThat(updatedInDb.getEmail()).isEqualTo("mariana.nova@test.com");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("DELETE /api/admin/customers/{id}: Deve remover cliente do banco de dados")
    void shouldDeleteCustomer() throws Exception {
        Customer saved = customerRepository.save(testCustomer);

        mockMvc.perform(delete("/api/admin/customers/" + saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(customerRepository.findById(saved.getId())).isEmpty();
    }
}
