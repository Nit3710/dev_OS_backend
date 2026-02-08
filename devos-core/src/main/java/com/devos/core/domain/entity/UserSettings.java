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
@Table(name = "user_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "theme")
    private String theme = "light";

    @Column(name = "language")
    private String language = "en";

    @Column(name = "timezone")
    private String timezone = "UTC";

    @Column(name = "notifications_enabled")
    private Boolean notificationsEnabled = true;

    @Column(name = "auto_save_enabled")
    private Boolean autoSaveEnabled = true;

    @Column(name = "code_font_size")
    private Integer codeFontSize = 14;

    @Column(name = "tab_size")
    private Integer tabSize = 4;

    @Column(name = "word_wrap")
    private Boolean wordWrap = true;

    @Column(name = "minimap_enabled")
    private Boolean minimapEnabled = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_preferences", columnDefinition = "json")
    private Map<String, Object> customPreferences;

    @Column(name = "default_llm_provider")
    private String defaultLlmProvider;

    @Column(name = "max_tokens_per_request")
    private Integer maxTokensPerRequest = 4000;

    @Column(name = "auto_index_projects")
    private Boolean autoIndexProjects = true;
}
