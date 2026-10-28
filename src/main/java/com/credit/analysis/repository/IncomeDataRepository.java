package com.credit.analysis.repository;

import com.credit.analysis.domain.entity.IncomeData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IncomeDataRepository extends JpaRepository<IncomeData, UUID> {
}
