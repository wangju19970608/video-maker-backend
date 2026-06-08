package com.example.video.admin.service;

import com.example.video.admin.dto.AdminUserDto;
import com.example.video.admin.dto.UserUpsertRequest;
import com.example.video.admin.model.AdminRole;
import com.example.video.admin.model.AdminUser;
import com.example.video.admin.repository.AdminRoleRepository;
import com.example.video.admin.repository.AdminUserRepository;
import com.example.video.exception.NotFoundException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final AdminUserRepository userRepository;
    private final AdminRoleRepository roleRepository;

    public AdminUserService(AdminUserRepository userRepository, AdminRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public List<AdminUserDto> listUsers(String keyword, Integer status) {
        Specification<AdminUser> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), likeKeyword),
                        cb.like(cb.lower(root.get("nickname")), likeKeyword)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(specification).stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminUserDto createUser(UserUpsertRequest request) {
        validateCreateRequest(request);

        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("username already exists");
        }

        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        user.setNickname(request.getNickname().trim());
        user.setEmail(trimToNull(request.getEmail()));
        user.setPhone(trimToNull(request.getPhone()));
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        user.setRoles(fetchRoles(request.getRoleIds()));
        
        user.setWechatQrUrl(trimToNull(request.getWechatQrUrl()));
        user.setWechatLink(trimToNull(request.getWechatLink()));
        user.setCustomerServiceActive(request.getCustomerServiceActive() != null ? request.getCustomerServiceActive() : false);
        user.setCustomerServiceWeight(request.getCustomerServiceWeight() != null ? request.getCustomerServiceWeight() : 1);

        return toDto(userRepository.save(user));
    }

    @Transactional
    public AdminUserDto updateUser(Long userId, UserUpsertRequest request) {
        AdminUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (StringUtils.hasText(request.getUsername())) {
            String username = request.getUsername().trim();
            userRepository.findByUsername(username)
                    .filter(other -> !other.getId().equals(userId))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("username already exists");
                    });
            user.setUsername(username);
        }

        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        }

        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname().trim());
        }

        user.setEmail(trimToNull(request.getEmail()));
        user.setPhone(trimToNull(request.getPhone()));

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        if (request.getRoleIds() != null) {
            user.setRoles(fetchRoles(request.getRoleIds()));
        }

        if (request.getWechatQrUrl() != null) {
             user.setWechatQrUrl(trimToNull(request.getWechatQrUrl()));
        }
        if (request.getWechatLink() != null) {
             user.setWechatLink(trimToNull(request.getWechatLink()));
        }
        if (request.getCustomerServiceActive() != null) {
            user.setCustomerServiceActive(request.getCustomerServiceActive());
        }
        if (request.getCustomerServiceWeight() != null) {
            user.setCustomerServiceWeight(request.getCustomerServiceWeight());
        }

        return toDto(userRepository.save(user));
    }

    @Transactional
    public AdminUserDto updateStatus(Long userId, Integer status) {
        AdminUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        user.setStatus(status == null ? 1 : status);
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        AdminUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException("default admin cannot be deleted");
        }
        userRepository.delete(user);
    }

    public AdminUser findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
    }

    public AdminUser findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    public boolean matchesPassword(String rawPassword, String hashPassword) {
        if (!StringUtils.hasText(hashPassword)) {
            return false;
        }
        if (hashPassword.startsWith("$2a$") || hashPassword.startsWith("$2b$") || hashPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, hashPassword);
        }
        return hashPassword.equals(rawPassword);
    }

    public AdminUserDto toDto(AdminUser user) {
        AdminUserDto dto = new AdminUserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt() == null ? "" : DATETIME_FORMATTER.format(user.getCreatedAt()));

        dto.setWechatQrUrl(user.getWechatQrUrl());
        dto.setWechatLink(user.getWechatLink());
        dto.setCustomerServiceActive(user.getCustomerServiceActive());
        dto.setCustomerServiceWeight(user.getCustomerServiceWeight());

        dto.setRoleIds(user.getRoles().stream().map(AdminRole::getId).collect(Collectors.toList()));
        dto.setRoleNames(user.getRoles().stream().map(AdminRole::getRoleName).collect(Collectors.toList()));
        return dto;
    }

    private void validateCreateRequest(UserUpsertRequest request) {
        if (!StringUtils.hasText(request.getUsername())) {
            throw new IllegalArgumentException("username is required");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("password is required");
        }
        if (!StringUtils.hasText(request.getNickname())) {
            throw new IllegalArgumentException("nickname is required");
        }
    }

    private Set<AdminRole> fetchRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(roleRepository.findAllById(roleIds));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}