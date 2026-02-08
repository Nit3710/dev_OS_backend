package com.devos.core.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "diff_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiffChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer chunkNumber;

    @Column(name = "old_start_line")
    private Integer oldStartLine;

    @Column(name = "old_line_count")
    private Integer oldLineCount;

    @Column(name = "new_start_line")
    private Integer newStartLine;

    @Column(name = "new_line_count")
    private Integer newLineCount;

    @Column(name = "chunk_header")
    private String chunkHeader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_change_id", nullable = false)
    private FileChange fileChange;

    @OneToMany(mappedBy = "diffChunk", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DiffLine> diffLines;

    @Column(name = "context_lines_before")
    private Integer contextLinesBefore = 3;

    @Column(name = "context_lines_after")
    private Integer contextLinesAfter = 3;

    @Column(name = "is_hunk")
    private Boolean isHunk = true;
}
