package com.credit.analysis.repository;

import com.credit.analysis.domain.entity.CreditAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CreditAnalysisRepository extends JpaRepository<CreditAnalysis, UUID> {
    List<CreditAnalysis> findByCustomerId(UUID customerId);
}
