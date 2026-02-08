package com.devos.core.repository;

import com.devos.core.domain.entity.AIMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AIMessageRepository extends JpaRepository<AIMessage, Long> {

    List<AIMessage> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Page<AIMessage> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);

    List<AIMessage> findByProjectIdAndThreadIdOrderByCreatedAt(Long projectId, String threadId);

    List<AIMessage> findByProjectIdAndTypeOrderByCreatedAtDesc(Long projectId, AIMessage.MessageType type);

    @Query("SELECT a FROM AIMessage a WHERE a.project.id = :projectId AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AIMessage> findRecentMessages(@Param("projectId") Long projectId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AIMessage a WHERE a.project.id = :projectId AND a.type = :type")
    long countByProjectAndType(@Param("projectId") Long projectId, @Param("type") AIMessage.MessageType type);

    @Query("SELECT SUM(a.tokenCount) FROM AIMessage a WHERE a.project.id = :projectId AND a.type = 'ASSISTANT'")
    Long getTotalTokensUsed(@Param("projectId") Long projectId);

    @Query("SELECT SUM(a.cost) FROM AIMessage a WHERE a.project.id = :projectId")
    Double getTotalCost(@Param("projectId") Long projectId);

    @Query("SELECT a FROM AIMessage a WHERE a.content LIKE %:search% AND a.project.id = :projectId")
    List<AIMessage> searchMessages(@Param("projectId") Long projectId, @Param("search") String search);
}
