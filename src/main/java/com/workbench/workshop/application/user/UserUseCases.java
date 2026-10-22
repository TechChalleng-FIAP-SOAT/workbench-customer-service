package com.workbench.workshop.application.user;

import com.workbench.workshop.application.user.port.out.UserRepository;
import com.workbench.workshop.domain.user.Role;
import com.workbench.workshop.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Casos de uso de identidade do staff (paridade com o SignUpUseCase/LoginUseCase
 * do monólito). O Customer Service é a fonte de verdade dos usuários; a emissão
 * do JWT fica na Lambda de auth, que valida as credenciais aqui via
 * {@link #authenticate(String, String)}.
 */
@Service
@RequiredArgsConstructor
public class UserUseCases {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Cadastra um usuário staff com senha BCrypt. */
    @Transactional
    public SignUpResult signUp(String email, String rawPassword, Set<String> roleNames) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("E-mail é obrigatório.");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("Senha é obrigatória (mínimo 6 caracteres).");
        }
        String normalizedEmail = email.strip().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Já existe um usuário com este e-mail.");
        }
        Set<Role> roles = parseRoles(roleNames);
        User user = User.create(normalizedEmail, passwordEncoder.encode(rawPassword), roles);
        User saved = userRepository.save(user);
        return new SignUpResult(saved.getId(), saved.getEmail(),
                saved.getRoles().stream().map(Role::name).collect(Collectors.toSet()));
    }

    /**
     * Valida e-mail/senha (usado pela Lambda de auth de staff). Retorna as roles
     * quando as credenciais conferem; lança {@link BadCredentialsException} caso
     * contrário — sem distinguir "usuário inexistente" de "senha errada".
     */
    @Transactional(readOnly = true)
    public AuthResult authenticate(String email, String rawPassword) {
        String normalizedEmail = email == null ? "" : email.strip().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .filter(User::isActive)
                .orElseThrow(BadCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException();
        }
        return new AuthResult(user.getEmail(),
                user.getRoles().stream().map(Role::name).collect(Collectors.toSet()));
    }

    private Set<Role> parseRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of(Role.TECHNICIAN);
        }
        try {
            return roleNames.stream()
                    .map(r -> Role.valueOf(r.strip().toUpperCase()))
                    .collect(Collectors.toSet());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Role inválida. Use ADMIN ou TECHNICIAN.");
        }
    }

    public record SignUpResult(Long id, String email, Set<String> roles) {}
    public record AuthResult(String email, Set<String> roles) {}

    /** Credenciais inválidas (mapeada para 401). */
    public static class BadCredentialsException extends RuntimeException {
        public BadCredentialsException() { super("Credenciais inválidas."); }
    }
}
