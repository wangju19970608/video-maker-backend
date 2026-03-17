package com.example.video.admin.service;

import com.example.video.admin.dto.MenuNodeDto;
import com.example.video.admin.dto.MenuUpsertRequest;
import com.example.video.admin.model.AdminMenu;
import com.example.video.admin.model.AdminRole;
import com.example.video.admin.repository.AdminMenuRepository;
import com.example.video.admin.repository.AdminRoleRepository;
import com.example.video.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminMenuService {

    private final AdminMenuRepository menuRepository;
    private final AdminRoleRepository roleRepository;

    public AdminMenuService(AdminMenuRepository menuRepository, AdminRoleRepository roleRepository) {
        this.menuRepository = menuRepository;
        this.roleRepository = roleRepository;
    }

    public List<MenuNodeDto> listTree(boolean onlyEnabled) {
        List<AdminMenu> menus = onlyEnabled
                ? menuRepository.findByStatusOrderBySortOrderAscIdAsc(1)
                : menuRepository.findAllByOrderBySortOrderAscIdAsc();
        return buildTree(menus);
    }

    public List<MenuNodeDto> listByIds(Set<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<AdminMenu> menus = menuRepository.findAllById(menuIds).stream()
                .sorted((a, b) -> {
                    int sortCompare = Integer.compare(a.getSortOrder(), b.getSortOrder());
                    if (sortCompare != 0) {
                        return sortCompare;
                    }
                    return Long.compare(a.getId(), b.getId());
                })
                .collect(Collectors.toList());
        return buildTree(menus);
    }

    @Transactional
    public MenuNodeDto create(MenuUpsertRequest request) {
        validateRequest(request);
        AdminMenu menu = new AdminMenu();
        fillMenu(menu, request);
        return toDto(menuRepository.save(menu));
    }

    @Transactional
    public MenuNodeDto update(Long menuId, MenuUpsertRequest request) {
        validateRequest(request);
        AdminMenu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new NotFoundException("Menu not found: " + menuId));
        fillMenu(menu, request);
        return toDto(menuRepository.save(menu));
    }

    @Transactional
    public void delete(Long menuId) {
        AdminMenu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new NotFoundException("Menu not found: " + menuId));

        List<AdminRole> roles = roleRepository.findAll();
        for (AdminRole role : roles) {
            Set<AdminMenu> roleMenus = new LinkedHashSet<>(role.getMenus());
            roleMenus.removeIf(item -> menuId.equals(item.getId()));
            role.setMenus(roleMenus);
        }
        roleRepository.saveAll(roles);

        List<AdminMenu> subMenus = menuRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(item -> menuId.equals(item.getParentId()))
                .collect(Collectors.toList());
        if (!subMenus.isEmpty()) {
            throw new IllegalArgumentException("Please delete child menus first");
        }

        menuRepository.delete(menu);
    }

    public MenuNodeDto toDto(AdminMenu menu) {
        MenuNodeDto dto = new MenuNodeDto();
        dto.setId(menu.getId());
        dto.setParentId(menu.getParentId());
        dto.setMenuName(menu.getMenuName());
        dto.setMenuType(menu.getMenuType());
        dto.setRoutePath(menu.getRoutePath());
        dto.setComponentPath(menu.getComponentPath());
        dto.setPermissionKey(menu.getPermissionKey());
        dto.setIcon(menu.getIcon());
        dto.setSortOrder(menu.getSortOrder());
        dto.setVisible(menu.getVisible());
        dto.setStatus(menu.getStatus());
        return dto;
    }

    private List<MenuNodeDto> buildTree(List<AdminMenu> menus) {
        Map<Long, MenuNodeDto> nodes = new LinkedHashMap<>();
        for (AdminMenu menu : menus) {
            nodes.put(menu.getId(), toDto(menu));
        }

        List<MenuNodeDto> roots = new ArrayList<>();
        for (AdminMenu menu : menus) {
            MenuNodeDto current = nodes.get(menu.getId());
            Long parentId = menu.getParentId();
            if (parentId == null || parentId == 0L || !nodes.containsKey(parentId)) {
                roots.add(current);
            } else {
                nodes.get(parentId).getChildren().add(current);
            }
        }
        return roots;
    }

    private void validateRequest(MenuUpsertRequest request) {
        if (!StringUtils.hasText(request.getMenuName())) {
            throw new IllegalArgumentException("menuName is required");
        }
        if (!StringUtils.hasText(request.getMenuType())) {
            throw new IllegalArgumentException("menuType is required");
        }
    }

    private void fillMenu(AdminMenu menu, MenuUpsertRequest request) {
        menu.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        menu.setMenuName(request.getMenuName().trim());
        menu.setMenuType(request.getMenuType().trim());
        menu.setRoutePath(trimToNull(request.getRoutePath()));
        menu.setComponentPath(trimToNull(request.getComponentPath()));
        menu.setPermissionKey(trimToNull(request.getPermissionKey()));
        menu.setIcon(trimToNull(request.getIcon()));
        menu.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        menu.setVisible(request.getVisible() == null ? true : request.getVisible());
        menu.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}