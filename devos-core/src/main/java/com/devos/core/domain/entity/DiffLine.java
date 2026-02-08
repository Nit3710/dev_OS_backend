package com.devos.core.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "diff_lines")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiffLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LineType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "old_line_number")
    private Integer oldLineNumber;

    @Column(name = "new_line_number")
    private Integer newLineNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diff_chunk_id", nullable = false)
    private DiffChunk diffChunk;

    @Column(name = "is_whitespace_only")
    private Boolean isWhitespaceOnly = false;

    @Column(name = "similarity_score")
    private Double similarityScore;

    public enum LineType {
        CONTEXT, ADDED, REMOVED
    }
}
