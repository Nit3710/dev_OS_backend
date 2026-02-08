package com.devos.core.repository;

import com.devos.core.domain.entity.FileNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileNodeRepository extends JpaRepository<FileNode, Long> {

    List<FileNode> findByProjectId(Long projectId);

    List<FileNode> findByProjectIdAndParentId(Long projectId, Long parentId);

    List<FileNode> findByProjectIdAndType(Long projectId, FileNode.FileType type);

    Optional<FileNode> findByProjectIdAndRelativePath(Long projectId, String relativePath);

    List<FileNode> findByProjectIdAndIsIndexedFalse(Long projectId);

    List<FileNode> findByProjectIdAndIsIgnoredFalse(Long projectId);

    @Query("SELECT f FROM FileNode f WHERE f.project.id = :projectId AND f.type = 'DIRECTORY' AND f.parent.id IS NULL")
    List<FileNode> findRootDirectories(@Param("projectId") Long projectId);

    @Query("SELECT f FROM FileNode f WHERE f.project.id = :projectId AND f.parent.id = :parentId ORDER BY f.type, f.name")
    List<FileNode> findByProjectAndParentOrderByTypeAndName(@Param("projectId") Long projectId, @Param("parentId") Long parentId);

    @Query("SELECT f FROM FileNode f WHERE f.project.id = :projectId AND f.name LIKE %:search% AND f.type = 'FILE'")
    List<FileNode> searchFiles(@Param("projectId") Long projectId, @Param("search") String search);

    @Query("SELECT f FROM FileNode f WHERE f.project.id = :projectId AND f.language = :language AND f.type = 'FILE'")
    List<FileNode> findByLanguage(@Param("projectId") Long projectId, @Param("language") String language);

    @Query("SELECT COUNT(f) FROM FileNode f WHERE f.project.id = :projectId AND f.type = 'FILE'")
    long countFilesByProject(@Param("projectId") Long projectId);

    @Query("SELECT f FROM FileNode f WHERE f.project.id = :projectId AND f.isBinary = false AND f.type = 'FILE'")
    List<FileNode> findTextFiles(@Param("projectId") Long projectId);

    List<FileNode> findByProjectIdOrderByRelativePathAsc(Long projectId);
}
