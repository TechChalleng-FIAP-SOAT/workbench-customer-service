package com.workbench.workshop.application.user;

import com.workbench.workshop.application.user.port.out.UserRepository;
import com.workbench.workshop.domain.user.Role;
import com.workbench.workshop.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valida e-mail/senha do staff (usado pela Lambda de auth antes de emitir o JWT).
 * Devolve as roles quando confere; senão {@link BadCredentialsException}, sem
 * distinguir "usuário inexistente" de "senha errada". Depois de várias falhas
 * seguidas o e-mail fica bloqueado por um tempo ({@link LoginAttemptLimiter}).
 */
@Service
@RequiredArgsConstructor
public class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptLimiter attemptLimiter;

    public record Result(String email, Set<String> roles) {}

    @Transactional(readOnly = true)
    public Result execute(String email, String rawPassword) {
        String normalizedEmail = email == null ? "" : email.strip().toLowerCase();
        if (attemptLimiter.isLocked(normalizedEmail)) {
            throw new BadCredentialsException();
        }
        Optional<User> user = userRepository.findByEmail(normalizedEmail).filter(User::isActive);
        if (user.isEmpty() || !passwordEncoder.matches(rawPassword, user.get().getPasswordHash())) {
            attemptLimiter.recordFailure(normalizedEmail);
            throw new BadCredentialsException();
        }
        attemptLimiter.recordSuccess(normalizedEmail);
        return new Result(user.get().getEmail(),
                user.get().getRoles().stream().map(Role::name).collect(Collectors.toSet()));
    }
}
