package com.workbench.workshop.domain.user;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * Usuário staff (ADMIN/TECHNICIAN). O Customer Service passa a ser o serviço de
 * identidade da plataforma (paridade com o monólito, que tinha cadastro/login de
 * staff). A senha é armazenada já com hash BCrypt — o domínio nunca guarda senha
 * em texto puro.
 */
public class User {

    private Long id;
    private String email;
    private String passwordHash;
    private Set<Role> roles;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected User() {}

    /** Cria um novo usuário staff. A senha já deve chegar com hash. */
    public static User create(String email, String passwordHash, Set<Role> roles) {
        Objects.requireNonNull(email, "email é obrigatório");
        Objects.requireNonNull(passwordHash, "senha é obrigatória");
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Pelo menos um papel (role) é obrigatório.");
        }
        User u = new User();
        u.email = email.strip().toLowerCase();
        u.passwordHash = passwordHash;
        u.roles = Set.copyOf(roles);
        u.active = true;
        return u;
    }

    public static User reconstitute(Long id, String email, String passwordHash, Set<Role> roles,
                                    boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        User u = new User();
        u.id = id;
        u.email = email;
        u.passwordHash = passwordHash;
        u.roles = roles;
        u.active = active;
        u.createdAt = createdAt;
        u.updatedAt = updatedAt;
        return u;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Set<Role> getRoles() { return roles; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
