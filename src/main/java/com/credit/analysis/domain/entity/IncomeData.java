package com.credit.analysis.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "income_data")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncomeData {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Column(name = "monthly_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyIncome;
    
    @Column(name = "employment_type")
    private String employmentType;
    
    @Column(name = "employer_name")
    private String employerName;
    
    @Column(name = "employment_time_years")
    private Integer employmentTimeYears;
    
    @Column(name = "other_income", precision = 15, scale = 2)
    private BigDecimal otherIncome;
    
    @Column(name = "existing_debts", precision = 15, scale = 2)
    private BigDecimal existingDebts;
    
    @Column(name = "credit_score")
    private Integer creditScore;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
