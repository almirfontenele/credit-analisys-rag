package com.credit.analysis.domain.entity;

import com.credit.analysis.domain.enums.CreditStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_analysis")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditAnalysis {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "income_data_id")
    private IncomeData incomeData;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditStatus status;
    
    @Column(name = "requested_amount", precision = 15, scale = 2)
    private BigDecimal requestedAmount;
    
    @Column(name = "declared_income", precision = 15, scale = 2)
    private BigDecimal declaredIncome;
    
    @Column(name = "approved_limit", precision = 15, scale = 2)
    private BigDecimal approvedLimit;
    
    @Column(name = "analysis_details", columnDefinition = "TEXT")
    private String analysisDetails;
    
    @Column(name = "rag_context", columnDefinition = "TEXT")
    private String ragContext;
    
    @Column(name = "ai_response", columnDefinition = "TEXT")
    private String aiResponse;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
