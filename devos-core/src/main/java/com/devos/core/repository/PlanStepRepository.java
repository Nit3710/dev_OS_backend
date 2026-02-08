package com.devos.core.repository;

import com.devos.core.domain.entity.PlanStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanStepRepository extends JpaRepository<PlanStep, Long> {
    
    List<PlanStep> findByActionPlanId(Long actionPlanId);
    
    PlanStep findByActionPlanIdAndStepNumber(Long actionPlanId, Integer stepNumber);
}
