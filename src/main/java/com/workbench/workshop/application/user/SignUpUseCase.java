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

/** Cadastra um usuário staff com senha BCrypt (paridade com o SignUpUseCase do monólito). */
@Service
@RequiredArgsConstructor
public class SignUpUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public record Result(Long id, String email, Set<String> roles) {}

    @Transactional
    public Result execute(String email, String rawPassword, Set<String> roleNames) {
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
        User saved = userRepository.save(User.create(normalizedEmail, passwordEncoder.encode(rawPassword),
                parseRoles(roleNames)));
        return new Result(saved.getId(), saved.getEmail(),
                saved.getRoles().stream().map(Role::name).collect(Collectors.toSet()));
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
}
