package com.example.video.admin.service;

import com.example.video.admin.dto.AdminRoleDto;
import com.example.video.admin.dto.RoleUpsertRequest;
import com.example.video.admin.model.AdminMenu;
import com.example.video.admin.model.AdminRole;
import com.example.video.admin.repository.AdminMenuRepository;
import com.example.video.admin.repository.AdminRoleRepository;
import com.example.video.admin.repository.AdminUserRepository;
import com.example.video.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminRoleService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AdminRoleRepository roleRepository;
    private final AdminMenuRepository menuRepository;
    private final AdminUserRepository userRepository;

    public AdminRoleService(AdminRoleRepository roleRepository,
                            AdminMenuRepository menuRepository,
                            AdminUserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.menuRepository = menuRepository;
        this.userRepository = userRepository;
    }

    public List<AdminRoleDto> listAll() {
        return roleRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminRoleDto create(RoleUpsertRequest request) {
        validateRequest(request, true);
        if (roleRepository.existsByRoleCode(request.getRoleCode().trim())) {
            throw new IllegalArgumentException("roleCode already exists");
        }

        AdminRole role = new AdminRole();
        fillRole(role, request, true);
        return toDto(roleRepository.save(role));
    }

    @Transactional
    public AdminRoleDto update(Long roleId, RoleUpsertRequest request) {
        validateRequest(request, false);
        AdminRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));

        if (StringUtils.hasText(request.getRoleCode())) {
            String roleCode = request.getRoleCode().trim();
            roleRepository.findByRoleCode(roleCode)
                    .filter(other -> !other.getId().equals(roleId))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("roleCode already exists");
                    });
        }

        fillRole(role, request, false);
        return toDto(roleRepository.save(role));
    }

    @Transactional
    public AdminRoleDto bindMenus(Long roleId, List<Long> menuIds) {
        AdminRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));

        Set<AdminMenu> menus = new LinkedHashSet<>(menuRepository.findAllById(menuIds));
        role.setMenus(menus);
        return toDto(roleRepository.save(role));
    }

    @Transactional
    public void delete(Long roleId) {
        AdminRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));

        long bindUsers = userRepository.countByRoles_Id(roleId);
        if (bindUsers > 0) {
            throw new IllegalArgumentException("Role is bound to users and cannot be deleted");
        }

        roleRepository.delete(role);
    }

    public AdminRole findById(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));
    }

    public AdminRoleDto toDto(AdminRole role) {
        AdminRoleDto dto = new AdminRoleDto();
        dto.setId(role.getId());
        dto.setRoleCode(role.getRoleCode());
        dto.setRoleName(role.getRoleName());
        dto.setDescription(role.getDescription());
        dto.setStatus(role.getStatus());
        dto.setCreatedAt(role.getCreatedAt() == null ? "" : DATETIME_FORMATTER.format(role.getCreatedAt()));
        dto.setMenuIds(role.getMenus().stream().map(AdminMenu::getId).collect(Collectors.toList()));
        return dto;
    }

    private void fillRole(AdminRole role, RoleUpsertRequest request, boolean createMode) {
        if (StringUtils.hasText(request.getRoleCode())) {
            role.setRoleCode(request.getRoleCode().trim());
        } else if (createMode) {
            throw new IllegalArgumentException("roleCode is required");
        }

        if (StringUtils.hasText(request.getRoleName())) {
            role.setRoleName(request.getRoleName().trim());
        } else if (createMode) {
            throw new IllegalArgumentException("roleName is required");
        }

        role.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());

        if (request.getMenuIds() != null) {
            Set<AdminMenu> menus = new LinkedHashSet<>(menuRepository.findAllById(request.getMenuIds()));
            role.setMenus(menus);
        }
    }

    private void validateRequest(RoleUpsertRequest request, boolean createMode) {
        if (createMode && !StringUtils.hasText(request.getRoleCode())) {
            throw new IllegalArgumentException("roleCode is required");
        }
        if (createMode && !StringUtils.hasText(request.getRoleName())) {
            throw new IllegalArgumentException("roleName is required");
        }
    }
}