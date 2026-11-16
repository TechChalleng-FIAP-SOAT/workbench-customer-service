package com.workbench.workshop.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Expõe o {@link PasswordEncoder} (BCrypt) para o cadastro e a validação de senha
 * do usuário staff. Usa apenas spring-security-crypto — sem o filtro de segurança
 * web (a proteção de rota é feita pelo API Gateway/authorizer).
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
