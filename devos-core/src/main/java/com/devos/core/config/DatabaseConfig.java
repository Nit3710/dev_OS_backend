package com.devos.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.sql.DataSource;
import java.util.Optional;

@Configuration
@EnableJpaRepositories(basePackages = "com.devos.core.repository")
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class DatabaseConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        return new AuditorAwareImpl();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    public static class AuditorAwareImpl implements AuditorAware<Long> {
        @Override
        public Optional<Long> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return Optional.empty();
            }
            
            // Assuming the principal is the user ID or contains user ID
            // This will need to be adjusted based on your authentication setup
            try {
                return Optional.of(Long.parseLong(authentication.getName()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
    }
}
