package com.devos.core.repository;

import com.devos.core.domain.entity.FileOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileOperationRepository extends JpaRepository<FileOperation, Long> {

    List<FileOperation> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<FileOperation> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<FileOperation> findByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, FileOperation.OperationStatus status);

    @Query("SELECT fo FROM FileOperation fo WHERE fo.projectId = :projectId AND fo.createdAt >= :since ORDER BY fo.createdAt DESC")
    List<FileOperation> findRecentOperations(@Param("projectId") Long projectId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(fo) FROM FileOperation fo WHERE fo.projectId = :projectId AND fo.status = :status")
    long countByProjectAndStatus(@Param("projectId") Long projectId, @Param("status") FileOperation.OperationStatus status);

    @Query("SELECT fo.type, COUNT(fo) FROM FileOperation fo WHERE fo.projectId = :projectId AND fo.createdAt >= :since GROUP BY fo.type")
    List<Object[]> getOperationStatistics(@Param("projectId") Long projectId, @Param("since") LocalDateTime since);

    Page<FileOperation> findByProjectIdAndCreatedAtBetween(Long projectId, LocalDateTime since, LocalDateTime until, Pageable pageable);
}
