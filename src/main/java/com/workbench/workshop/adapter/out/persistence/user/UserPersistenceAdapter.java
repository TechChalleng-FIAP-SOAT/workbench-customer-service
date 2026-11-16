package com.workbench.workshop.adapter.out.persistence.user;

import com.workbench.workshop.application.user.port.out.UserRepository;
import com.workbench.workshop.domain.user.User;
import com.workbench.workshop.infrastructure.persistence.user.UserEntity;
import com.workbench.workshop.infrastructure.persistence.user.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    @Override
    public User save(User user) {
        UserEntity entity = (user.getId() != null)
                ? jpaRepository.findById(user.getId()).orElseGet(UserEntity::new)
                : new UserEntity();
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setRoles(new HashSet<>(user.getRoles()));
        entity.setActive(user.isActive());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    private User toDomain(UserEntity e) {
        return User.reconstitute(e.getId(), e.getEmail(), e.getPasswordHash(),
                new HashSet<>(e.getRoles()), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
