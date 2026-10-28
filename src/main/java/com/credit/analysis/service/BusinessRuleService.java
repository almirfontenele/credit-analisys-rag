package com.credit.analysis.service;

import com.credit.analysis.domain.dto.BusinessRuleDTO;
import com.credit.analysis.domain.entity.BusinessRule;
import com.credit.analysis.repository.BusinessRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class BusinessRuleService {
    private final BusinessRuleRepository repository;
    private final EmbeddingService embeddingService;
    
    public BusinessRuleService(BusinessRuleRepository repository, EmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }
    
    @Transactional
    public BusinessRule createRule(BusinessRuleDTO dto) {
        String combinedText = dto.getRuleName() + " " + dto.getRuleDescription() + " " + dto.getRuleContent();
        float[] embedding = embeddingService.createEmbedding(combinedText);
        
        BusinessRule rule = BusinessRule.builder()
                .ruleName(dto.getRuleName())
                .ruleDescription(dto.getRuleDescription())
                .ruleContent(dto.getRuleContent())
                .embedding(embedding)
                .build();
        return repository.save(rule);
    }
    
    public List<BusinessRule> findAll() { return repository.findAll(); }
    
    public BusinessRule findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Regra não encontrada"));
    }
    
    public List<BusinessRule> findSimilarRules(String query, int limit) {
        float[] queryEmbedding = embeddingService.createEmbedding(query);
        return repository.findSimilarRules(queryEmbedding, limit);
    }
    
    @Transactional
    public BusinessRule updateRule(UUID id, BusinessRuleDTO dto) {
        BusinessRule existing = findById(id);
        String combinedText = dto.getRuleName() + " " + dto.getRuleDescription() + " " + dto.getRuleContent();
        float[] embedding = embeddingService.createEmbedding(combinedText);
        
        existing.setRuleName(dto.getRuleName());
        existing.setRuleDescription(dto.getRuleDescription());
        existing.setRuleContent(dto.getRuleContent());
        existing.setEmbedding(embedding);
        return repository.save(existing);
    }
    
    @Transactional
    public void deleteRule(UUID id) { repository.deleteById(id); }
}
