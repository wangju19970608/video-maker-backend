package com.example.video.admin.repository;

import com.example.video.admin.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long>, JpaSpecificationExecutor<AdminUser> {

    Optional<AdminUser> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByRoles_Id(Long roleId);
}