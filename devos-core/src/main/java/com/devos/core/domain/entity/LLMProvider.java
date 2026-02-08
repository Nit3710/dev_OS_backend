package com.devos.core.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "llm_providers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LLMProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType type;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "max_tokens")
    private Integer maxTokens = 4000;

    @Column(name = "temperature")
    private Double temperature = 0.7;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_config", columnDefinition = "json")
    private Map<String, Object> additionalConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tokens_used")
    private Long tokensUsed = 0L;

    @Column(name = "cost_limit")
    private Double costLimit;

    @Column(name = "current_cost")
    private Double currentCost = 0.0;

    public enum ProviderType {
        OPENAI, ANTHROPIC, LOCAL, CUSTOM
    }
}
