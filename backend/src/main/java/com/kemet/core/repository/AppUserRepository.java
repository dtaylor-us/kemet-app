package com.kemet.core.repository;

import com.kemet.core.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByAuth0Subject(String auth0Subject);
}
