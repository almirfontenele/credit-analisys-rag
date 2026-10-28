package com.credit.analysis.controller;

import com.credit.analysis.domain.dto.BusinessRuleDTO;
import com.credit.analysis.domain.entity.BusinessRule;
import com.credit.analysis.service.BusinessRuleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/business-rules")
public class BusinessRuleController {
    private final BusinessRuleService service;
    
    public BusinessRuleController(BusinessRuleService service) { this.service = service; }
    
    @PostMapping
    public ResponseEntity<BusinessRule> createRule(@Valid @RequestBody BusinessRuleDTO dto) {
        return ResponseEntity.status(201).body(service.createRule(dto));
    }
    
    @GetMapping
    public ResponseEntity<List<BusinessRule>> getAllRules() {
        return ResponseEntity.ok(service.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<BusinessRule> getRuleById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<BusinessRule>> searchSimilarRules(
            @RequestParam String query, @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(service.findSimilarRules(query, limit));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<BusinessRule> updateRule(@PathVariable UUID id, @Valid @RequestBody BusinessRuleDTO dto) {
        return ResponseEntity.ok(service.updateRule(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        service.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
