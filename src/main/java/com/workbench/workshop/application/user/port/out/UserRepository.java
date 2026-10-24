package com.workbench.workshop.application.user.port.out;

import com.workbench.workshop.domain.user.User;

import java.util.Optional;

/** Porta de persistência do usuário staff. */
public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
