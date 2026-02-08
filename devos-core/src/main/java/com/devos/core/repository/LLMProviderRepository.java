package com.devos.core.repository;

import com.devos.core.domain.entity.LLMProvider;
import com.devos.core.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LLMProviderRepository extends JpaRepository<LLMProvider, Long> {
    
    Optional<LLMProvider> findByUserAndIsDefaultTrue(User user);
}
