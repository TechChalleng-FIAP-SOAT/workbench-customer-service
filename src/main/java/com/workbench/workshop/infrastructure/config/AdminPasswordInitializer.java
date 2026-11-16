package com.workbench.workshop.infrastructure.config;

import com.workbench.workshop.application.user.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Troca a senha padrão do admin semeado pela migration V2 ({@code admin123}) pela de
 * {@code ADMIN_INITIAL_PASSWORD}, no start. Só age enquanto o admin ainda usa a senha
 * padrão — uma senha já trocada nunca é sobrescrita.
 *
 * <p>Fora do perfil local ({@code admin.initial-password-required=true}, o padrão), o
 * serviço <b>não sobe</b> com o admin na senha padrão e sem uma senha válida: falhar o
 * start é melhor que expor {@code admin123}. No perfil local, apenas avisa.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminPasswordInitializer implements ApplicationRunner {

    static final String SEEDED_ADMIN = "admin@workbench.dev";
    static final String SEEDED_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.initial-password:}")
    private String initialPassword;

    @Value("${admin.initial-password-required:true}")
    private boolean initialPasswordRequired;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userRepository.findByEmail(SEEDED_ADMIN)
                .filter(admin -> passwordEncoder.matches(SEEDED_PASSWORD, admin.getPasswordHash()))
                .ifPresent(admin -> {
                    if (initialPassword == null || initialPassword.isBlank()) {
                        refuse("Admin " + SEEDED_ADMIN + " ainda usa a senha padrão; defina ADMIN_INITIAL_PASSWORD.");
                        return;
                    }
                    if (initialPassword.length() < 8) {
                        refuse("ADMIN_INITIAL_PASSWORD ignorada: use ao menos 8 caracteres.");
                        return;
                    }
                    admin.changePassword(passwordEncoder.encode(initialPassword));
                    userRepository.save(admin);
                    log.info("Senha padrão do admin {} substituída pela de ADMIN_INITIAL_PASSWORD.", SEEDED_ADMIN);
                });
    }

    private void refuse(String reason) {
        if (initialPasswordRequired) {
            throw new IllegalStateException(reason + " O serviço não sobe com a senha padrão fora do perfil local.");
        }
        log.warn(reason);
    }
}
