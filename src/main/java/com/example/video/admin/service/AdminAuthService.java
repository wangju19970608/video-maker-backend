package com.example.video.admin.service;

import com.example.video.admin.dto.AdminUserDto;
import com.example.video.admin.dto.LoginRequest;
import com.example.video.admin.dto.LoginResponse;
import com.example.video.admin.dto.MenuNodeDto;
import com.example.video.admin.model.AdminMenu;
import com.example.video.admin.model.AdminRole;
import com.example.video.admin.model.AdminUser;
import com.example.video.admin.security.AdminSessionService;
import com.example.video.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminAuthService {

    private final AdminUserService userService;
    private final AdminSessionService sessionService;

    public AdminAuthService(AdminUserService userService, AdminSessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
    }

    public LoginResponse login(LoginRequest request) {
        AdminUser user = userService.findByUsername(request.getUsername());
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new IllegalArgumentException("User is disabled");
        }
        if (!userService.matchesPassword(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = sessionService.createToken(user.getId(), user.getUsername());
        return buildLoginResponse(user, token);
    }

    public LoginResponse me(Long userId, String token) {
        AdminUser user = userService.findById(userId);
        return buildLoginResponse(user, token);
    }

    public void logout(String token) {
        sessionService.remove(token);
    }

    private LoginResponse buildLoginResponse(AdminUser user, String token) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);

        AdminUserDto userDto = userService.toDto(user);
        response.setUser(userDto);
        response.setMenus(buildMenuTree(user));
        return response;
    }

    private List<MenuNodeDto> buildMenuTree(AdminUser user) {
        Set<AdminMenu> menuSet = new LinkedHashSet<>();
        for (AdminRole role : user.getRoles()) {
            if (role.getStatus() != null && role.getStatus() != 1) {
                continue;
            }
            for (AdminMenu menu : role.getMenus()) {
                if (menu.getStatus() != null && menu.getStatus() == 1 && Boolean.TRUE.equals(menu.getVisible())) {
                    menuSet.add(menu);
                }
            }
        }

        List<AdminMenu> orderedMenus = new ArrayList<>(menuSet);
        orderedMenus.sort((a, b) -> {
            int sortCompare = Integer.compare(a.getSortOrder(), b.getSortOrder());
            if (sortCompare != 0) {
                return sortCompare;
            }
            return Long.compare(a.getId(), b.getId());
        });

        Map<Long, MenuNodeDto> nodes = new HashMap<>();
        for (AdminMenu menu : orderedMenus) {
            MenuNodeDto node = new MenuNodeDto();
            node.setId(menu.getId());
            node.setParentId(menu.getParentId());
            node.setMenuName(menu.getMenuName());
            node.setMenuType(menu.getMenuType());
            node.setRoutePath(menu.getRoutePath());
            node.setComponentPath(menu.getComponentPath());
            node.setPermissionKey(menu.getPermissionKey());
            node.setIcon(menu.getIcon());
            node.setSortOrder(menu.getSortOrder());
            node.setVisible(menu.getVisible());
            node.setStatus(menu.getStatus());
            nodes.put(menu.getId(), node);
        }

        List<MenuNodeDto> roots = new ArrayList<>();
        for (AdminMenu menu : orderedMenus) {
            MenuNodeDto current = nodes.get(menu.getId());
            if (menu.getParentId() == null || menu.getParentId() == 0L || !nodes.containsKey(menu.getParentId())) {
                roots.add(current);
            } else {
                nodes.get(menu.getParentId()).getChildren().add(current);
            }
        }
        return roots;
    }
}