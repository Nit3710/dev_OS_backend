package com.devos.api.controller;

import com.devos.api.dto.ActionPlanDto;
import com.devos.api.dto.PlanStepDto;
import com.devos.core.domain.entity.ActionPlan;
import com.devos.core.service.ActionPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai/plans")
@RequiredArgsConstructor
@Slf4j
public class ActionPlanController {

    private final ActionPlanService actionPlanService;
    private final com.devos.core.service.ActionExecutorService actionExecutorService;

    @GetMapping("/{planId}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<ActionPlanDto> getActionPlan(@PathVariable Long planId) {
        ActionPlan actionPlan = actionPlanService.getActionPlan(planId);
        return ResponseEntity.ok(ActionPlanDto.from(actionPlan));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<List<ActionPlanDto>> getProjectPlans(@PathVariable Long projectId) {
        List<ActionPlan> plans = actionPlanService.getProjectActionPlans(projectId);
        List<ActionPlanDto> planDtos = plans.stream()
                .map(ActionPlanDto::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(planDtos);
    }

    @PostMapping("/{planId}/approve")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<ActionPlanDto> approveActionPlan(@PathVariable Long planId) {
        ActionPlan actionPlan = actionPlanService.approveActionPlan(planId);
        log.info("Action plan approved: {}", planId);
        return ResponseEntity.ok(ActionPlanDto.from(actionPlan));
    }

    @PostMapping("/{planId}/apply")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<ActionPlanDto> executeActionPlan(@PathVariable Long planId) {
        ActionPlan actionPlan = actionExecutorService.executePlan(planId);
        log.info("Action plan execution completed: {}", planId);
        return ResponseEntity.ok(ActionPlanDto.from(actionPlan));
    }

    @PostMapping("/{planId}/steps/{stepId}/execute")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Void> executeStep(
            @PathVariable Long planId,
            @PathVariable Long stepId) {
        
        actionExecutorService.executeStep(stepId);
        log.info("Step execution completed: {}", stepId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{planId}/rollback")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<ActionPlanDto> rollbackActionPlan(@PathVariable Long planId) {
        ActionPlan actionPlan = actionPlanService.rollbackActionPlan(planId);
        log.info("Action plan rollback started: {}", planId);
        return ResponseEntity.ok(ActionPlanDto.from(actionPlan));
    }

    @PostMapping("/{planId}/pause")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<ActionPlanDto> pauseActionPlan(@PathVariable Long planId) {
        ActionPlan actionPlan = actionPlanService.pauseActionPlan(planId);
        log.info("Action plan paused: {}", planId);
        return ResponseEntity.ok(ActionPlanDto.from(actionPlan));
    }

    @PostMapping("/{planId}/resume")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<ActionPlanDto> resumeActionPlan(@PathVariable Long planId) {
        ActionPlan actionPlan = actionPlanService.resumeActionPlan(planId);
        log.info("Action plan resumed: {}", planId);
        return ResponseEntity.ok(ActionPlanDto.from(actionPlan));
    }

    @GetMapping("/{planId}/steps")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<List<PlanStepDto>> getPlanSteps(@PathVariable Long planId) {
        var steps = actionPlanService.getPlanSteps(planId);
        List<PlanStepDto> stepDtos = steps.stream()
                .map(PlanStepDto::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(stepDtos);
    }

    @PostMapping("/{planId}/steps/{stepId}/retry")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<PlanStepDto> retryStep(
            @PathVariable Long planId,
            @PathVariable Long stepId) {
        
        var step = actionPlanService.retryStep(planId, stepId);
        log.info("Step retry initiated: {} for plan: {}", stepId, planId);
        return ResponseEntity.ok(PlanStepDto.from(step));
    }

    @DeleteMapping("/{planId}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteActionPlan(@PathVariable Long planId) {
        actionPlanService.deleteActionPlan(planId);
        log.info("Action plan deleted: {}", planId);
        return ResponseEntity.noContent().build();
    }
}
