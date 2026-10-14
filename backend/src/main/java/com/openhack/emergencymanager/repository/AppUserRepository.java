package com.openhack.emergencymanager.repository;

import com.openhack.emergencymanager.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByRole(AppUser.Role role);
    List<AppUser> findAllByOrderByCreatedAtDesc();
}
