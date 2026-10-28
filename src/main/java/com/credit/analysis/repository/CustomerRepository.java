package com.credit.analysis.repository;

import com.credit.analysis.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByCpf(String cpf);
    Optional<Customer> findByCpfAndActiveTrue(String cpf);
    boolean existsByCpf(String cpf);
}
