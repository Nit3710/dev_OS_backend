package com.devos.core.service;

import com.devos.core.domain.entity.ActionPlan;
import com.devos.core.domain.entity.PlanStep;

import java.util.List;

public interface ActionPlanService {
    
    ActionPlan createActionPlan(ActionPlan actionPlan);
    
    ActionPlan getActionPlan(Long id);
    
    List<ActionPlan> getProjectActionPlans(Long projectId);
    
    ActionPlan updateActionPlan(Long id, ActionPlan actionPlan);
    
    void deleteActionPlan(Long id);
    
    ActionPlan approveActionPlan(Long id);
    
    ActionPlan executeActionPlan(Long id);
    
    ActionPlan rollbackActionPlan(Long id);
    
    ActionPlan pauseActionPlan(Long id);
    
    ActionPlan resumeActionPlan(Long id);
    
    List<PlanStep> getPlanSteps(Long planId);
    
    PlanStep retryStep(Long planId, Long stepId);
}
