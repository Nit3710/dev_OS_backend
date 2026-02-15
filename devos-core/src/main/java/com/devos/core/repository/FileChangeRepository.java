package com.devos.core.repository;

import com.devos.core.domain.entity.FileChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileChangeRepository extends JpaRepository<FileChange, Long> {
    
    List<FileChange> findByPlanStepId(Long planStepId);
    
    List<FileChange> findByActionPlanId(Long actionPlanId);
}
