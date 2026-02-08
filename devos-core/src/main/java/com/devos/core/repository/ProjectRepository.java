package com.devos.core.repository;

import com.devos.core.domain.entity.Project;
import com.devos.core.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByUser(User user);

    List<Project> findByUserId(Long userId);

    List<Project> findByUserAndStatus(User user, Project.ProjectStatus status);

    List<Project> findByUserIdAndStatus(Long userId, Project.ProjectStatus status);

    Optional<Project> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT p FROM Project p WHERE p.user.id = :userId AND p.status != 'DELETED'")
    List<Project> findActiveProjectsByUser(@Param("userId") Long userId);

    @Query("SELECT p FROM Project p WHERE p.isIndexed = false AND p.status = 'ACTIVE'")
    List<Project> findUnindexedProjects();

    @Query("SELECT COUNT(p) FROM Project p WHERE p.user.id = :userId AND p.status = 'ACTIVE'")
    long countActiveProjectsByUser(@Param("userId") Long userId);

    @Query("SELECT p FROM Project p WHERE p.name LIKE %:search% OR p.description LIKE %:search% AND p.user.id = :userId")
    List<Project> searchProjects(@Param("search") String search, @Param("userId") Long userId);
}
