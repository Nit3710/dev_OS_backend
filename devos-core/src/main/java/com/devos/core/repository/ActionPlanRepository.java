package com.devos.core.repository;

import com.devos.core.domain.entity.ActionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActionPlanRepository extends JpaRepository<ActionPlan, Long> {

    List<ActionPlan> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ActionPlan> findByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, ActionPlan.PlanStatus status);

    Optional<ActionPlan> findByProjectIdAndId(Long projectId, Long id);

    @Query("SELECT a FROM ActionPlan a WHERE a.project.id = :projectId AND a.status IN :statuses ORDER BY a.createdAt DESC")
    List<ActionPlan> findByProjectIdAndStatusIn(@Param("projectId") Long projectId, @Param("statuses") List<ActionPlan.PlanStatus> statuses);

    @Query("SELECT COUNT(a) FROM ActionPlan a WHERE a.project.id = :projectId AND a.status = :status")
    long countByProjectAndStatus(@Param("projectId") Long projectId, @Param("status") ActionPlan.PlanStatus status);

    @Query("SELECT a FROM ActionPlan a WHERE a.status = 'PENDING_APPROVAL' ORDER BY a.createdAt ASC")
    List<ActionPlan> findPendingApproval();

    @Query("SELECT a FROM ActionPlan a WHERE a.project.id = :projectId AND a.title LIKE %:search% OR a.description LIKE %:search%")
    List<ActionPlan> searchActionPlans(@Param("projectId") Long projectId, @Param("search") String search);

    @Query("SELECT a FROM ActionPlan a WHERE a.executedAt >= :since AND a.status IN ('COMPLETED', 'FAILED')")
    List<ActionPlan> findRecentlyExecuted(@Param("since") java.time.LocalDateTime since);
}
