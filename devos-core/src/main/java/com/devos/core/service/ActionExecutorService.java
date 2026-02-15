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
    
    /**
     * Executes a specific step of an action plan.
     * 
     * @param stepId The ID of the step to execute
}
