package com.devos.core.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "file_nodes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "relative_path", nullable = false)
    private String relativePath;

    @Column(name = "absolute_path", nullable = false)
    private String absolutePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileType type;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "last_modified")
    private Long lastModified;

    @Column(name = "is_binary")
    private Boolean isBinary = false;

    @Column(name = "language")
    private String language;

    @Column(name = "is_indexed")
    private Boolean isIndexed = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    private String metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FileNode parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<FileNode> children;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "line_count")
    private Integer lineCount;

    @Column(name = "is_ignored")
    private Boolean isIgnored = false;

    public enum FileType {
        FILE, DIRECTORY
    }
}
