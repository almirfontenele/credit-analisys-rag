package com.credit.analysis.repository;

import com.credit.analysis.domain.entity.BusinessRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Repository
public interface BusinessRuleRepository extends JpaRepository<BusinessRule, UUID> {

    @Query(value = "SELECT * FROM business_rules ORDER BY embedding <-> cast(:queryEmbedding as vector) LIMIT :limit", 
           nativeQuery = true)
    List<BusinessRule> findSimilarRules(@Param("queryEmbedding") String queryEmbedding, 
                                        @Param("limit") int limit);

    default List<BusinessRule> findSimilarRules(float[] queryEmbedding, int limit) {
        return findSimilarRules(Arrays.toString(queryEmbedding), limit);
    }
}
