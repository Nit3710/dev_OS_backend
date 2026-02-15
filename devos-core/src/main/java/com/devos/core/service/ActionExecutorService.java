package com.devos.core.service;

import com.devos.core.domain.entity.ActionPlan;

public interface ActionExecutorService {
    
    /**
     * Executes the given action plan.
     * 
     * @param actionPlanId The ID of the action plan to execute
     * @return The executed action plan
     */
    ActionPlan executePlan(Long actionPlanId);
    
    void executeStep(Long stepId);
}
