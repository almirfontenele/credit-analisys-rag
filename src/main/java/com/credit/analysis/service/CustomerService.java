package com.credit.analysis.service;

import com.credit.analysis.domain.dto.CustomerDTO;
import com.credit.analysis.domain.entity.Customer;
import com.credit.analysis.exception.CustomerNotFoundException;
import com.credit.analysis.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerService {
    private final CustomerRepository repository;
    
    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }
    
    @Transactional
    public Customer createCustomer(CustomerDTO dto) {
        if (repository.existsByCpf(dto.getCpf())) {
            throw new RuntimeException("CPF já cadastrado");
        }
        Customer customer = Customer.builder()
                .name(dto.getName())
                .cpf(dto.getCpf())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .birthDate(dto.getBirthDate())
                .address(dto.getAddress())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
        return repository.save(customer);
    }
    
    public List<Customer> findAll() { return repository.findAll(); }
    
    public Customer findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new CustomerNotFoundException("Cliente não encontrado"));
    }
    
    public Customer findByCpf(String cpf) {
        return repository.findByCpf(cpf).orElseThrow(() -> new CustomerNotFoundException("Cliente não encontrado"));
    }

    public Customer getAuthenticatedCustomer(String username) {
        String clean = username != null ? username.replaceAll("\\D", "") : "";
        if (!clean.isEmpty()) {
            Optional<Customer> byCpf = repository.findByCpf(clean);
            if (byCpf.isPresent()) return byCpf.get();
        }
        return repository.findByCpf(username)
                .or(() -> repository.findAll().stream().filter(c -> username.equalsIgnoreCase(c.getEmail()) || username.equalsIgnoreCase(c.getName())).findFirst())
                .or(() -> repository.findAll().stream().findFirst())
                .orElseThrow(() -> new CustomerNotFoundException("Nenhum cliente cadastrado no sistema"));
    }
    
    @Transactional
    public Customer updateCustomer(UUID id, CustomerDTO dto) {
        Customer existing = findById(id);
        existing.setName(dto.getName());
        existing.setEmail(dto.getEmail());
        existing.setPhone(dto.getPhone());
        existing.setBirthDate(dto.getBirthDate());
        existing.setAddress(dto.getAddress());
        if (dto.getActive() != null) {
            existing.setActive(dto.getActive());
        }
        return repository.save(existing);
    }

    @Transactional
    public Customer updateCustomerProfile(UUID id, CustomerDTO dto) {
        Customer existing = findById(id);
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getEmail() != null) existing.setEmail(dto.getEmail());
        if (dto.getPhone() != null) existing.setPhone(dto.getPhone());
        if (dto.getAddress() != null) existing.setAddress(dto.getAddress());
        if (dto.getBirthDate() != null) existing.setBirthDate(dto.getBirthDate());
        return repository.save(existing);
    }

    @Transactional
    public Customer updateStatus(UUID id, boolean active) {
        Customer customer = findById(id);
        customer.setActive(active);
        return repository.save(customer);
    }
    
    @Transactional
    public void deleteCustomer(UUID id) { repository.deleteById(id); }
}
