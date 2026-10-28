package com.credit.analysis.controller;

import com.credit.analysis.domain.dto.CustomerDTO;
import com.credit.analysis.domain.entity.Customer;
import com.credit.analysis.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Customers", description = "Endpoints para gerenciamento de clientes")
public class CustomerController {
    private final CustomerService service;
    
    public CustomerController(CustomerService service) { this.service = service; }
    
    @GetMapping("/api/customers/me")
    @Operation(summary = "Consultar perfil do cliente autenticado")
    public ResponseEntity<Customer> getMyProfile(Principal principal) {
        String username = principal != null ? principal.getName() : "customer";
        return ResponseEntity.ok(service.getAuthenticatedCustomer(username));
    }

    @PutMapping("/api/customers/me")
    @Operation(summary = "Atualizar perfil cadastral do cliente autenticado")
    public ResponseEntity<Customer> updateMyProfile(Principal principal, @RequestBody CustomerDTO dto) {
        String username = principal != null ? principal.getName() : "customer";
        Customer customer = service.getAuthenticatedCustomer(username);
        return ResponseEntity.ok(service.updateCustomerProfile(customer.getId(), dto));
    }

    @PostMapping({"/api/operators/customers", "/api/admin/customers", "/api/customers"})
    @Operation(summary = "Criar novo cliente")
    public ResponseEntity<Customer> createCustomer(@Valid @RequestBody CustomerDTO dto) {
        return ResponseEntity.status(201).body(service.createCustomer(dto));
    }
    
    @GetMapping({"/api/operators/customers", "/api/admin/customers", "/api/customers"})
    @Operation(summary = "Listar clientes")
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(service.findAll());
    }
    
    @GetMapping({"/api/operators/customers/{id}", "/api/admin/customers/{id}", "/api/customers/{id}"})
    @Operation(summary = "Obter cliente por ID")
    public ResponseEntity<Customer> getCustomerById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }
    
    @GetMapping({"/api/operators/customers/cpf/{cpf}", "/api/customers/cpf/{cpf}"})
    @Operation(summary = "Obter cliente por CPF")
    public ResponseEntity<Customer> getCustomerByCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(service.findByCpf(cpf));
    }
    
    @PutMapping({"/api/operators/customers/{id}", "/api/admin/customers/{id}", "/api/customers/{id}"})
    @Operation(summary = "Atualizar cliente por ID")
    public ResponseEntity<Customer> updateCustomer(@PathVariable UUID id, @Valid @RequestBody CustomerDTO dto) {
        return ResponseEntity.ok(service.updateCustomer(id, dto));
    }

    @PutMapping("/api/admin/customers/{id}/status")
    @Operation(summary = "Ativar ou inativar cadastro de cliente (Admin)")
    public ResponseEntity<Customer> updateStatus(@PathVariable UUID id, @RequestParam boolean active) {
        return ResponseEntity.ok(service.updateStatus(id, active));
    }
    
    @DeleteMapping({"/api/operators/customers/{id}", "/api/admin/customers/{id}"})
    @Operation(summary = "Remover cliente por ID")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        service.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
