package com.credit.analysis.controller;

import com.credit.analysis.domain.dto.CreditApplicationRequestDTO;
import com.credit.analysis.domain.dto.CreditApplicationResponseDTO;
import com.credit.analysis.domain.dto.CreditAnalysisResponse;
import com.credit.analysis.domain.dto.IncomeDataDTO;
import com.credit.analysis.domain.entity.CreditAnalysis;
import com.credit.analysis.domain.entity.IncomeData;
import com.credit.analysis.service.RAGCreditAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/operators/credit-analysis", "/api/credit-analysis"})
@Tag(name = "Credit Analysis", description = "Endpoints para análise e solicitação de crédito")
public class CreditAnalysisController {
    private final RAGCreditAnalysisService service;
    
    public CreditAnalysisController(RAGCreditAnalysisService service) { this.service = service; }
    
    @PostMapping(path = {"", "/solicitar", "/solicitacao", "/request"})
    @Operation(summary = "Solicitar crédito validando regras de negócio")
    public ResponseEntity<CreditApplicationResponseDTO> applyForCredit(
            @Valid @RequestBody CreditApplicationRequestDTO request) {
        return ResponseEntity.ok(service.applyForCredit(request));
    }

    @PostMapping("/analyze/{customerId}")
    @Operation(summary = "Analisar crédito por ID do cliente")
    public ResponseEntity<CreditAnalysisResponse> analyzeCredit(
            @PathVariable UUID customerId, @Valid @RequestBody IncomeDataDTO dto) {
        IncomeData incomeData = service.createIncomeData(dto);
        return ResponseEntity.ok(service.analyzeCredit(customerId, incomeData));
    }
    
    @GetMapping("/history/{customerId}")
    @Operation(summary = "Consultar histórico de análises do cliente")
    public ResponseEntity<List<CreditAnalysis>> getAnalysisHistory(@PathVariable UUID customerId) {
        return ResponseEntity.ok(service.getCustomerAnalyses(customerId));
    }
}
