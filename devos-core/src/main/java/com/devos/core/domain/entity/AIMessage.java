package com.devos.core.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "ai_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    private Map<String, Object> metadata;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "llm_provider_id")
    private LLMProvider llmProvider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_plan_id")
    private ActionPlan actionPlan;

    @Column(name = "thread_id")
    private String threadId;

    @Column(name = "parent_message_id")
    private Long parentMessageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "cost")
    private Double cost;

    @Column(name = "is_streaming")
    private Boolean isStreaming = false;

    @Column(name = "stream_id")
    private String streamId;

    public enum MessageType {
        USER, ASSISTANT, SYSTEM, ERROR
    }
}
