package com.limvik.econome.global.config;

import jakarta.persistence.EntityListeners;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableJpaAuditing
@EnableTransactionManagement
@EntityListeners({AuditingEntityListener.class})
@Configuration
public class JpaAuditConfig {
}

