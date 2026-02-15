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
    public ResponseEntity<ActionPlanDto> getActionPlan(
            @RequestHeader("Authorization") String token,
            @PathVariable Long planId) {
        
        String jwtToken = token.replace("Bearer ", "");
        ActionPlan actionPlan = actionPlanService.getActionPlan(planId, jwtToken);
        
        return ResponseEntity.ok(ActionPlanDto.from(actionPlan));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<List<ActionPlanDto>> getProjectPlans(
            @RequestHeader("Authorization") String token,
            @PathVariable Long projectId) {
        
        String jwtToken = token.replace("Bearer ", "");
        List<ActionPlan> plans = actionPlanService.getProjectActionPlans(projectId, jwtToken);
        List<ActionPlanDto> planDtos = plans.stream()
                .map(ActionPlanDto::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(planDtos);
    }

    @PostMapping("/{planId}/approve")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<ActionPlanDto> approveActionPlan(
            @RequestHeader("Authorization") String token,
            @PathVariable Long planId) {
        
        String jwtToken = token.replace("Bearer ", "");
        ActionPlan actionPlan = actionPlanService.approveActionPlan(planId, jwtToken);
        
        log.info("Action plan approved: {}", planId);
        return ResponseEntity.ok(ActionPlanDto.from(actionPlan));
    }

    @PostMapping("/{planId}/apply")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<ActionPlanDto> executeActionPlan(
            @RequestHeader("Authorization") String token,
            @PathVariable Long planId) {
        
        String jwtToken = token.replace("Bearer ", "");
        // Delegate to executor service which handles the actual logic + status updates
        ActionPlan actionPlan = actionExecutorService.executePlan(planId, jwtToken);
        
        log.info("Action plan execution completed: {}", planId);
        return ResponseEntity.ok(ActionPlanDto.from(actionPlan));
    }

    @PostMapping("/{planId}/rollback")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<ActionPlanDto> rollbackActionPlan(
            @RequestHeader("Authorization") String token,
            @PathVariable Long planId) {
        
        String jwtToken = token.replace("Bearer ", "");
        ActionPlan actionPlan = actionPlanService.rollbackActionPlan(planId, jwtToken);
        
        log.info("Action plan rollback started: {}", planId);
        return ResponseEntity.ok(ActionPlanDto.from(actionPlan));
    }

    @PostMapping("/{planId}/pause")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<ActionPlanDto> pauseActionPlan(
            @RequestHeader("Authorization") String token,
            @PathVariable Long planId) {
        
        String jwtToken = token.replace("Bearer ", "");
        ActionPlan actionPlan = actionPlanService.pauseActionPlan(planId, jwtToken);
        
        log.info("Action plan paused: {}", planId);
        return ResponseEntity.ok(ActionPlanDto.from(actionPlan));
    }

    @PostMapping("/{planId}/resume")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<ActionPlanDto> resumeActionPlan(
            @RequestHeader("Authorization") String token,
            @PathVariable Long planId) {
        
        String jwtToken = token.replace("Bearer ", "");
        ActionPlan actionPlan = actionPlanService.resumeActionPlan(planId, jwtToken);
        
        log.info("Action plan resumed: {}", planId);
        return ResponseEntity.ok(ActionPlanDto.from(actionPlan));
    }

    @GetMapping("/{planId}/steps")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<List<PlanStepDto>> getPlanSteps(
            @RequestHeader("Authorization") String token,
            @PathVariable Long planId) {
        
        String jwtToken = token.replace("Bearer ", "");
        var steps = actionPlanService.getPlanSteps(planId, jwtToken);
        List<PlanStepDto> stepDtos = steps.stream()
                .map(PlanStepDto::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(stepDtos);
    }

    @PostMapping("/{planId}/steps/{stepId}/retry")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<PlanStepDto> retryStep(
            @RequestHeader("Authorization") String token,
            @PathVariable Long planId,
            @PathVariable Long stepId) {
        
        String jwtToken = token.replace("Bearer ", "");
        var step = actionPlanService.retryStep(planId, stepId, jwtToken);
        
        log.info("Step retry initiated: {} for plan: {}", stepId, planId);
        return ResponseEntity.ok(PlanStepDto.from(step));
    }

    @DeleteMapping("/{planId}")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteActionPlan(
            @RequestHeader("Authorization") String token,
            @PathVariable Long planId) {
        
        String jwtToken = token.replace("Bearer ", "");
        actionPlanService.deleteActionPlan(planId, jwtToken);
        
        log.info("Action plan deleted: {}", planId);
        return ResponseEntity.noContent().build();
    }
}
