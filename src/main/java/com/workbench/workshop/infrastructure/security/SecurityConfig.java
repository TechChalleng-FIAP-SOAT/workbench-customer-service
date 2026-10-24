package com.workbench.workshop.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Segurança do Customer Service (defesa em profundidade: cada serviço valida o JWT).
 *
 * <p>Rotas públicas (sem token):
 * <ul>
 *   <li>health/actuator e Swagger/OpenAPI;</li>
 *   <li>{@code POST /api/users/auth/validate} — a Lambda de auth valida credenciais
 *       aqui ANTES de emitir o token, então não há JWT nesse momento.</li>
 * </ul>
 * {@code POST /api/users/signup} exige ADMIN (só staff cria usuário). Consultas de
 * cliente/veículo são liberadas ao cliente dono (ROLE_CLIENT) e ao staff; o restante
 * (escrita) fica com o staff.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) { this.jwtFilter = jwtFilter; }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // A Lambda de auth valida credenciais aqui antes de emitir o token.
                        .requestMatchers(HttpMethod.POST, "/api/users/auth/validate").permitAll()
                        // Cadastro de usuário staff: só ADMIN.
                        .requestMatchers(HttpMethod.POST, "/api/users/signup").hasRole("ADMIN")
                        // Consultas de cliente/veículo: cliente dono + staff.
                        .requestMatchers(HttpMethod.GET, "/api/clients/**", "/api/vehicles/**")
                            .hasAnyRole("CLIENT", "USER", "ADMIN", "TECHNICIAN")
                        .anyRequest().hasAnyRole("ADMIN", "TECHNICIAN"))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, exAuth) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exAuth.getMessage())))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
