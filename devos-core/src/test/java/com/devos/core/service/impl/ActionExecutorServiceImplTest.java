package com.devos.core.service.impl;

import com.devos.core.domain.entity.ActionPlan;
import com.devos.core.domain.entity.PlanStep;
import com.devos.core.domain.entity.Project;
import com.devos.core.repository.ActionPlanRepository;
import com.devos.core.repository.FileChangeRepository;
import com.devos.core.repository.PlanStepRepository;
import com.devos.core.repository.ProjectRepository;
import com.devos.file.service.FileService;
import com.devos.file.service.GitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionExecutorServiceImplTest {

    @Mock
    private ActionPlanRepository actionPlanRepository;
    @Mock
    private PlanStepRepository planStepRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private FileService fileService;
    @Mock
    private GitService gitService;
    @Mock
    private FileChangeRepository fileChangeRepository;
    @Mock
    private com.devos.core.service.AuthService authService;

    @InjectMocks
    private ActionExecutorServiceImpl actionExecutorService;

    private ActionPlan actionPlan;
    private PlanStep planStep;
    private Project project;
    private final String TEST_TOKEN = "test-token";

    @BeforeEach
    void setUp() {
        com.devos.core.domain.entity.User user = com.devos.core.domain.entity.User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        project = Project.builder()
                .id(1L)
                .name("Test Project")
                .localPath("/tmp/test-project")
                .user(user)
                .build();

        actionPlan = ActionPlan.builder()
                .id(1L)
                .project(project)
                .status(ActionPlan.PlanStatus.APPROVED)
                .title("Test Plan")
                .build();

        Map<String, Object> params = new HashMap<>();
        params.put("path", "src/Main.java");
        params.put("content", "public class Main {}");

        planStep = new PlanStep();
        planStep.setId(1L);
        planStep.setActionPlan(actionPlan);
        planStep.setStepNumber(1);
        planStep.setType(PlanStep.StepType.CREATE_FILE);
        planStep.setStatus(PlanStep.StepStatus.PENDING);
        planStep.setParameters(params);
        planStep.setTitle("Create Main.java");
    }

    @Test
    void executePlan_Success() {
        when(authService.getCurrentUser()).thenReturn(project.getUser());
        when(actionPlanRepository.findById(1L)).thenReturn(Optional.of(actionPlan));
        when(planStepRepository.findByActionPlanId(1L)).thenReturn(Collections.singletonList(planStep));
        when(planStepRepository.findById(1L)).thenReturn(Optional.of(planStep));
        when(actionPlanRepository.save(any(ActionPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActionPlan result = actionExecutorService.executePlan(1L);

        assertNotNull(result);
        assertEquals(ActionPlan.PlanStatus.COMPLETED, result.getStatus());
        verify(planStepRepository, atLeastOnce()).save(any(PlanStep.class));
        verify(fileService).createFile(eq(1L), eq("src/Main.java"), eq("public class Main {}"));
        verify(fileChangeRepository).save(any());
    }

    @Test
    void executePlan_InvalidStatus() {
        actionPlan.setStatus(ActionPlan.PlanStatus.DRAFT);
        when(authService.getCurrentUser()).thenReturn(project.getUser());
        when(actionPlanRepository.findById(1L)).thenReturn(Optional.of(actionPlan));

        assertThrows(IllegalStateException.class, () -> actionExecutorService.executePlan(1L));
    }

    @Test
    void executeStep_CreateFile() {
        when(planStepRepository.findById(1L)).thenReturn(Optional.of(planStep));

        actionExecutorService.executeStep(1L);

        verify(fileService).createFile(eq(1L), eq("src/Main.java"), eq("public class Main {}"));
        verify(fileChangeRepository).save(any());
    }
}
