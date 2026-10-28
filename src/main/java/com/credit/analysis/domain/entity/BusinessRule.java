package com.credit.analysis.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "business_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessRule {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;
    
    @Column(name = "rule_name", nullable = false)
    private String ruleName;
    
    @Column(name = "rule_description", nullable = false, columnDefinition = "TEXT")
    private String ruleDescription;
    
    @Column(name = "rule_content", nullable = false, columnDefinition = "TEXT")
    private String ruleContent;
    
    @Convert(converter = com.credit.analysis.config.PgVectorConverter.class)
    @org.hibernate.annotations.ColumnTransformer(write = "?::vector", read = "embedding::text")
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private float[] embedding;
    
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
