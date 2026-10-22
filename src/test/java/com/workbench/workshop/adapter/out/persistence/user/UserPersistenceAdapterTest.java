package com.workbench.workshop.adapter.out.persistence.user;

import com.workbench.workshop.domain.user.Role;
import com.workbench.workshop.domain.user.User;
import com.workbench.workshop.infrastructure.persistence.user.UserEntity;
import com.workbench.workshop.infrastructure.persistence.user.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserPersistenceAdapterTest {

    private UserJpaRepository jpa;
    private UserPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        jpa = mock(UserJpaRepository.class);
        adapter = new UserPersistenceAdapter(jpa);
        when(jpa.save(any(UserEntity.class))).thenAnswer(i -> {
            UserEntity e = i.getArgument(0);
            if (e.getId() == null) e.setId(1L);
            return e;
        });
    }

    @Test
    void saveMapsDomainToEntityAndBack() {
        User user = User.create("staff@x.com", "$2a$10$hash", Set.of(Role.ADMIN));
        User saved = adapter.save(user);

        assertEquals(1L, saved.getId());
        assertEquals("staff@x.com", saved.getEmail());
        assertTrue(saved.getRoles().contains(Role.ADMIN));
        verify(jpa).save(any(UserEntity.class));
    }

    @Test
    void findByEmailMapsEntityToDomain() {
        UserEntity e = new UserEntity();
        e.setId(5L);
        e.setEmail("a@x.com");
        e.setPasswordHash("$2a$10$h");
        e.setRoles(new java.util.HashSet<>(Set.of(Role.TECHNICIAN)));
        e.setActive(true);
        when(jpa.findByEmail("a@x.com")).thenReturn(Optional.of(e));

        Optional<User> result = adapter.findByEmail("a@x.com");

        assertTrue(result.isPresent());
        assertEquals(5L, result.get().getId());
        assertTrue(result.get().getRoles().contains(Role.TECHNICIAN));
    }

    @Test
    void existsByEmailDelegates() {
        when(jpa.existsByEmail("x@x.com")).thenReturn(true);
        assertTrue(adapter.existsByEmail("x@x.com"));
    }
}
