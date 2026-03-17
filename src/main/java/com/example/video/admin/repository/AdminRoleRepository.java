package com.example.video.admin.repository;

import com.example.video.admin.model.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRoleRepository extends JpaRepository<AdminRole, Long> {

    Optional<AdminRole> findByRoleCode(String roleCode);

    boolean existsByRoleCode(String roleCode);
}