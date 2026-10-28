package com.credit.analysis.service;

import com.credit.analysis.domain.dto.CreditApplicationRequestDTO;
import com.credit.analysis.domain.dto.CreditApplicationResponseDTO;
import com.credit.analysis.domain.dto.CreditAnalysisResponse;
import com.credit.analysis.domain.dto.IncomeDataDTO;
import com.credit.analysis.domain.entity.*;
import com.credit.analysis.domain.enums.CreditStatus;
import com.credit.analysis.exception.CustomerInactiveException;
import com.credit.analysis.exception.CustomerNotFoundException;
import com.credit.analysis.repository.CreditAnalysisRepository;
import com.credit.analysis.repository.CustomerRepository;
import com.credit.analysis.repository.IncomeDataRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RAGCreditAnalysisService {
    private final CreditAnalysisRepository analysisRepository;
    private final BusinessRuleService businessRuleService;
    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final IncomeDataRepository incomeDataRepository;
    private final ChatClient chatClient;
    
    public RAGCreditAnalysisService(CreditAnalysisRepository analysisRepository,
                                    BusinessRuleService businessRuleService,
                                    CustomerService customerService,
                                    CustomerRepository customerRepository,
                                    IncomeDataRepository incomeDataRepository,
                                    ChatClient.Builder chatClientBuilder) {
        this.analysisRepository = analysisRepository;
        this.businessRuleService = businessRuleService;
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.incomeDataRepository = incomeDataRepository;
        this.chatClient = chatClientBuilder.build();
    }

    public record CreditEvaluationResult(
            CreditStatus status,
            BigDecimal approvedLimit,
            String analysisDetails,
            String aiResponse
    ) {}

    @Transactional
    public CreditApplicationResponseDTO applyForCredit(CreditApplicationRequestDTO request) {
        String rawCpf = request.getCpf();
        String cleanCpf = rawCpf != null ? rawCpf.replaceAll("\\D", "") : "";

        // TASK-007: Validação cadastral ativa e remoção da auto-criação de cliente fictício
        Customer customer = customerRepository.findByCpf(cleanCpf)
                .or(() -> customerRepository.findByCpf(rawCpf))
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Solicitante não possui cadastro no sistema. Regularize seu cadastro antes de solicitar crédito."));

        if (customer.getActive() != null && !customer.getActive()) {
            throw new CustomerInactiveException(
                    "Cadastro do cliente encontra-se inativo no sistema.");
        }

        // Recuperação semântica das regras via RAG (pgvector)
        String query = String.format("concessão de credito limite valor solicitado em relacao ao salario %.2f valor solicitado %.2f",
                request.getSalario(), request.getValorCreditoSolicitado());
        List<BusinessRule> relevantRules = businessRuleService.findSimilarRules(query, 5);
        if (relevantRules == null || relevantRules.isEmpty()) {
            relevantRules = businessRuleService.findAll();
        }

        String ragContext = relevantRules != null ? relevantRules.stream()
                .map(rule -> String.format("Regra: %s\nDescrição: %s\nConteúdo: %s",
                        rule.getRuleName(), rule.getRuleDescription(), rule.getRuleContent()))
                .collect(Collectors.joining("\n\n---\n\n")) : "";

        // TASK-008: Avaliação da solicitação pelo Agente de IA com parecer fundamentado
        CreditEvaluationResult evaluation = evaluateCreditRequest(
                relevantRules, request.getSalario(), request.getValorCreditoSolicitado(), customer, ragContext);

        IncomeData incomeData = IncomeData.builder()
                .customer(customer)
                .monthlyIncome(request.getSalario())
                .build();
        incomeData = incomeDataRepository.save(incomeData);

        // TASK-009: Persistência integral no PostgreSQL (Aprovadas e Reprovadas)
        CreditAnalysis analysis = CreditAnalysis.builder()
                .customer(customer)
                .incomeData(incomeData)
                .status(evaluation.status())
                .requestedAmount(request.getValorCreditoSolicitado())
                .declaredIncome(request.getSalario())
                .approvedLimit(evaluation.approvedLimit())
                .analysisDetails(evaluation.analysisDetails())
                .ragContext(ragContext)
                .aiResponse(evaluation.aiResponse())
                .build();
        analysis = analysisRepository.save(analysis);

        // TASK-010: Padronização da resposta DTO
        String message = evaluation.status() == CreditStatus.APPROVED
                ? "Crédito aprovado e cadastrado com sucesso"
                : "Crédito reprovado de acordo com as políticas vigentes";

        return CreditApplicationResponseDTO.builder()
                .analysisId(analysis.getId())
                .customerId(customer.getId())
                .customerName(customer.getName())
                .cpf(customer.getCpf())
                .salario(request.getSalario())
                .valorCreditoSolicitado(request.getValorCreditoSolicitado())
                .approvedLimit(evaluation.approvedLimit())
                .status(evaluation.status())
                .message(message)
                .analysisDetails(evaluation.analysisDetails())
                .build();
    }

    private CreditEvaluationResult evaluateCreditRequest(List<BusinessRule> rules,
                                                         BigDecimal salario,
                                                         BigDecimal valorSolicitado,
                                                         Customer customer,
                                                         String ragContext) {
        BigDecimal maxAllowedRatio = findMaxRatioFromRules(rules);
        boolean ratioExceeded = false;
        BigDecimal maxCreditAllowed = BigDecimal.ZERO;
        if (maxAllowedRatio != null) {
            maxCreditAllowed = salario.multiply(maxAllowedRatio);
            if (valorSolicitado.compareTo(maxCreditAllowed) > 0) {
                ratioExceeded = true;
            }
        }

        try {
            String prompt = String.format("""
                    Você é um analista e validador de crédito bancário especialista.
                    
                    REGRAS DE CRÉDITO RECUPERADAS:
                    %s
                    
                    DADOS DA SOLICITAÇÃO:
                    Cliente: %s (CPF: %s)
                    Salário informado: R$ %.2f
                    Valor de Crédito Solicitado: R$ %.2f
                    
                    INSTRUÇÕES:
                    Analise rigorosamente a compatibilidade do valor de crédito solicitado com o salário informado e com as regras vigentes.
                    Se o valor solicitado for incompatível com as regras ou capacidade de renda, decida por REJECTED e forneça a motivação detalhada.
                    Se estiver de acordo, decida por APPROVED.
                    
                    FORMATO OBRIGATÓRIO DA RESPOSTA:
                    STATUS: [APPROVED|REJECTED]
                    LIMITE: [valor numérico]
                    MOTIVACAO: [justificativa analítica detalhada com as regras e cálculos]
                    """,
                    ragContext, customer.getName(), customer.getCpf(), salario, valorSolicitado);

            String aiResponse = chatClient.prompt().user(prompt).call().content();
            if (aiResponse != null) {
                String upper = aiResponse.toUpperCase();
                boolean aiRejected = upper.contains("STATUS: REJECTED") || upper.contains("APTO: NAO") || upper.contains("APTO: NÃO")
                        || upper.contains("INCOMPATIVEL") || upper.contains("INCOMPATÍVEL");

                if (ratioExceeded || aiRejected) {
                    String reason = extractMotivacao(aiResponse);
                    if (reason == null || reason.isBlank()) {
                        reason = ratioExceeded
                                ? String.format("Reprovado: O valor solicitado de R$ %.2f excede o limite máximo permitido de R$ %.2f (%.0f%% da renda) estipulado pelas regras normativas.",
                                        valorSolicitado, maxCreditAllowed, maxAllowedRatio.multiply(BigDecimal.valueOf(100)).doubleValue())
                                : "Reprovado: Solicitação não atende aos critérios das políticas de crédito vigentes.";
                    }
                    return new CreditEvaluationResult(CreditStatus.REJECTED, BigDecimal.ZERO, reason, aiResponse);
                }

                if (upper.contains("STATUS: APPROVED") || upper.contains("APTO: SIM")) {
                    String reason = extractMotivacao(aiResponse);
                    if (reason == null || reason.isBlank()) {
                        reason = "Crédito aprovado conforme regras de negócio e política de concessão de crédito.";
                    }
                    return new CreditEvaluationResult(CreditStatus.APPROVED, valorSolicitado, reason, aiResponse);
                }
            }
        } catch (Exception e) {
            // Em caso de indisponibilidade da IA, aplica a verificação estrita das regras normativas
        }

        if (ratioExceeded) {
            String reason = String.format("Reprovado: O valor solicitado de R$ %.2f excede o limite máximo permitido de R$ %.2f (%.0f%% da renda) estipulado pelas regras normativas.",
                    valorSolicitado, maxCreditAllowed, maxAllowedRatio.multiply(BigDecimal.valueOf(100)).doubleValue());
            return new CreditEvaluationResult(CreditStatus.REJECTED, BigDecimal.ZERO, reason, "APTO: NAO - Limite de comprometimento de renda excedido");
        }

        return new CreditEvaluationResult(CreditStatus.APPROVED, valorSolicitado,
                "Crédito aprovado conforme regras de negócio", "APTO: SIM");
    }

    private String extractMotivacao(String response) {
        if (response == null) return null;
        Matcher matcher = Pattern.compile("(?:MOTIVACAO|JUSTIFICATIVA):\\s*(.+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private BigDecimal findMaxRatioFromRules(List<BusinessRule> rules) {
        if (rules != null) {
            for (BusinessRule rule : rules) {
                String content = rule.getRuleContent();
                if (content == null) continue;
                Matcher matcher = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:%|do sal[aá]rio)", Pattern.CASE_INSENSITIVE).matcher(content);
                if (matcher.find()) {
                    String valStr = matcher.group(1).replace(",", ".");
                    BigDecimal val = new BigDecimal(valStr);
                    if (val.compareTo(BigDecimal.ONE) > 0) {
                        val = val.divide(BigDecimal.valueOf(100));
                    }
                    return val;
                }
            }
        }
        return new BigDecimal("0.33");
    }
    
    @Transactional
    public IncomeData createIncomeData(IncomeDataDTO dto) {
        Customer customer = customerService.findById(dto.getCustomerId());
        IncomeData incomeData = IncomeData.builder()
                .customer(customer)
                .monthlyIncome(dto.getMonthlyIncome())
                .employmentType(dto.getEmploymentType())
                .employerName(dto.getEmployerName())
                .employmentTimeYears(dto.getEmploymentTimeYears())
                .otherIncome(dto.getOtherIncome() != null ? dto.getOtherIncome() : BigDecimal.ZERO)
                .existingDebts(dto.getExistingDebts() != null ? dto.getExistingDebts() : BigDecimal.ZERO)
                .creditScore(dto.getCreditScore())
                .build();
        return incomeDataRepository.save(incomeData);
    }
    
    @Transactional
    public CreditAnalysisResponse analyzeCredit(UUID customerId, IncomeData incomeData) {
        Customer customer = customerService.findById(customerId);
        String queryContext = buildQueryContext(incomeData);
        List<BusinessRule> relevantRules = businessRuleService.findSimilarRules(queryContext, 5);
        
        String ragContext = relevantRules.stream()
                .map(rule -> String.format("Regra: %s\nDescrição: %s\nConteúdo: %s",
                        rule.getRuleName(), rule.getRuleDescription(), rule.getRuleContent()))
                .collect(Collectors.joining("\n\n---\n\n"));
        
        String prompt = buildPrompt(customer, incomeData, ragContext);
        String aiResponse = chatClient.prompt().user(prompt).call().content();
        
        CreditStatus status = parseStatus(aiResponse);
        BigDecimal approvedLimit = parseLimit(aiResponse);
        
        CreditAnalysis analysis = CreditAnalysis.builder()
                .customer(customer).incomeData(incomeData)
                .status(status).approvedLimit(approvedLimit)
                .analysisDetails(aiResponse).ragContext(ragContext).aiResponse(aiResponse)
                .build();
        analysis = analysisRepository.save(analysis);
        
        return CreditAnalysisResponse.builder()
                .analysisId(analysis.getId()).customerId(customer.getId())
                .customerName(customer.getName()).status(analysis.getStatus())
                .approvedLimit(analysis.getApprovedLimit())
                .analysisDetails(analysis.getAnalysisDetails())
                .aiResponse(analysis.getAiResponse())
                .build();
    }
    
    private String buildQueryContext(IncomeData incomeData) {
        return String.format("Análise de crédito: renda R$ %.2f, emprego %s, %d anos, dívidas R$ %.2f, score %d",
                incomeData.getMonthlyIncome(), incomeData.getEmploymentType(),
                incomeData.getEmploymentTimeYears(), incomeData.getExistingDebts(),
                incomeData.getCreditScore());
    }
    
    private String buildPrompt(Customer customer, IncomeData incomeData, String ragContext) {
        return String.format("""
                Você é um analista de crédito. Analise os dados e determine se o cliente está habilitado para crédito.
                
                CLIENTE: %s (CPF: %s, Nascimento: %s)
                Renda: R$ %.2f | Emprego: %s (%s) | Tempo: %d anos
                Outras Rendas: R$ %.2f | Dívidas: R$ %.2f | Score: %d
                
                REGRAS APLICÁVEIS:
                %s
                
                RESPOSTA NO FORMATO:
                STATUS: [APPROVED|REJECTED|PENDING_REVIEW]
                LIMITE: [valor numérico]
                JUSTIFICATIVA: [explicação]
                """,
                customer.getName(), customer.getCpf(), customer.getBirthDate(),
                incomeData.getMonthlyIncome(), incomeData.getEmploymentType(),
                incomeData.getEmployerName(), incomeData.getEmploymentTimeYears(),
                incomeData.getOtherIncome(), incomeData.getExistingDebts(),
                incomeData.getCreditScore(), ragContext);
    }
    
    private CreditStatus parseStatus(String response) {
        if (response.contains("APPROVED")) return CreditStatus.APPROVED;
        if (response.contains("REJECTED")) return CreditStatus.REJECTED;
        return CreditStatus.PENDING_REVIEW;
    }
    
    private BigDecimal parseLimit(String response) {
        Matcher matcher = Pattern.compile("LIMITE:\\s*R?\\$?\\s*([\\d.,]+)").matcher(response);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1).replace(".", "").replace(",", "."));
        }
        return BigDecimal.ZERO;
    }
    
    public List<CreditAnalysis> getCustomerAnalyses(UUID customerId) {
        return analysisRepository.findByCustomerId(customerId);
    }
}
