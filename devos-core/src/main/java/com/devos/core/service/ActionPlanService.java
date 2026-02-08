package com.devos.core.service;

import com.devos.core.domain.entity.ActionPlan;
import com.devos.core.domain.entity.PlanStep;

import java.util.List;

public interface ActionPlanService {
    
    ActionPlan createActionPlan(ActionPlan actionPlan, String token);
    
    ActionPlan getActionPlan(Long id, String token);
    
    List<ActionPlan> getProjectActionPlans(Long projectId, String token);
    
    ActionPlan updateActionPlan(Long id, ActionPlan actionPlan, String token);
    
    void deleteActionPlan(Long id, String token);
    
    ActionPlan approveActionPlan(Long id, String token);
    
    ActionPlan executeActionPlan(Long id, String token);
    
    ActionPlan rollbackActionPlan(Long id, String token);
    
    ActionPlan pauseActionPlan(Long id, String token);
    
    ActionPlan resumeActionPlan(Long id, String token);
    
    List<PlanStep> getPlanSteps(Long planId, String token);
    
    PlanStep retryStep(Long planId, Long stepId, String token);
}
