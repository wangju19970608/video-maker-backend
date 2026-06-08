package com.example.video.service;

import com.example.video.admin.model.AdminMenu;
import com.example.video.admin.model.AdminRole;
import com.example.video.admin.model.AdminUser;
import com.example.video.admin.repository.AdminMenuRepository;
import com.example.video.admin.repository.AdminRoleRepository;
import com.example.video.admin.repository.AdminUserRepository;
import com.example.video.model.OrderRecord;
import com.example.video.model.VideoTemplate;
import com.example.video.repository.OrderRecordRepository;
import com.example.video.repository.VideoTemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final VideoTemplateRepository templateRepository;
    private final OrderRecordRepository orderRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminMenuRepository adminMenuRepository;

    public DataInitializer(VideoTemplateRepository templateRepository,
                           OrderRecordRepository orderRepository,
                           AdminUserRepository adminUserRepository,
                           AdminRoleRepository adminRoleRepository,
                           AdminMenuRepository adminMenuRepository) {
        this.templateRepository = templateRepository;
        this.orderRepository = orderRepository;
        this.adminUserRepository = adminUserRepository;
        this.adminRoleRepository = adminRoleRepository;
        this.adminMenuRepository = adminMenuRepository;
    }

    @Override
    public void run(String... args) {
        initTemplates();
        initOrders();
        initAdminMenus();
        initAdminRoles();
        initAdminUsers();
    }

    private void initTemplates() {
        templateRepository.findByTemplateCode("video/1").orElseGet(() -> {
            VideoTemplate tpl = buildTemplate(
                "video/1",
                "奥特曼生日祝福（视频版）",
                "上传照片自动生成奥特曼视频",
                "hero",
                "奥特曼",
                "blessing",
                "祝福视频",
                "新品",
                "奥特曼生日主题自带动效",
                new BigDecimal("29.90"),
                "#3550f3",
                "#0f1d67",
                0,
                true
            );
            tpl.setTemplateType("video");
            tpl.setCoverUrl("/api/assets/templates/video/1.mp4");
            return templateRepository.save(tpl);
        });

        if (templateRepository.count() > 1) {
            templateRepository.findByTemplateCode("TPL-9001")
                    .orElseGet(() -> {
                        VideoTemplate screenTemplate = buildTemplate(
                                "TPL-9001",
                                "生日派对大屏祝福",
                                "适合宴会投屏，动态粒子",
                                "screen",
                                "生日投屏",
                                "tv",
                                "电视投屏图",
                                "高清",
                                "生日会现场投屏模板",
                                new BigDecimal("15.90"),
                                "#ff4b54",
                                "#9a031e",
                                5,
                                true
                        );
                        screenTemplate.setCoverUrl("/api/assets/templates/1.png");
                        screenTemplate.setPreviewUrl("template/1.docx");
                        return templateRepository.save(screenTemplate);
                    });
            return;
        }

        List<VideoTemplate> templates = new ArrayList<>();
        VideoTemplate screenTemplate = buildTemplate(
                "TPL-9001",
                "生日派对大屏祝福",
                "适合宴会投屏，动态粒子",
                "screen",
                "生日投屏",
                "tv",
                "电视投屏图",
                "高清",
                "生日会现场投屏模板",
                new BigDecimal("15.90"),
                "#ff4b54",
                "#9a031e",
                5,
                true
        );
        screenTemplate.setCoverUrl("/api/assets/templates/1.png");
        screenTemplate.setPreviewUrl("template/1.docx");
        templates.add(screenTemplate);

        templates.add(buildTemplate(
                "TPL-3761",
                "乔迁大屏祝福",
                "红金国风，适配宴会投屏",
                "housewarming",
                "乔迁",
                "tv",
                "电视投屏图",
                "精选",
                "适配大屏的乔迁投屏模板",
                new BigDecimal("19.90"),
                "#c90b18",
                "#81000f",
                10,
                true
        ));

        templates.add(buildTemplate(
                "TPL-3762",
                "乔迁邀请函",
                "竖屏邀请函模板，支持多图",
                "housewarming",
                "乔迁",
                "invitation",
                "邀请函",
                "热卖",
                "用于乔迁宴会邀请场景",
                new BigDecimal("19.90"),
                "#a40b14",
                "#5f000a",
                20,
                true
        ));

        templates.add(buildTemplate(
                "TPL-4102",
                "生日投屏烟花祝福",
                "适合宴会投屏，动效焰火",
                "screen",
                "生日投屏",
                "tv",
                "电视投屏图",
                "高清",
                "生日会现场投屏祝福模板",
                new BigDecimal("15.90"),
                "#ff4b54",
                "#9a031e",
                30,
                true
        ));

        templates.add(buildTemplate(
                "TPL-4518",
                "升学典礼荣誉时刻",
                "学业成长主题，支持多图轮播",
                "study",
                "升学",
                "blessing",
                "祝福视频",
                "新品",
                "升学季祝福短视频模板",
                new BigDecimal("16.90"),
                "#2f6cd6",
                "#10366f",
                40,
                true
        ));

        templates.add(buildTemplate(
                "TPL-4820",
                "温馨生日祝福视频",
                "家庭相册版，支持背景音乐",
                "birthday",
                "生日祝福",
                "blessing",
                "祝福视频",
                "热门",
                "家庭相册类型生日模板",
                new BigDecimal("12.90"),
                "#ff8e53",
                "#a7135f",
                50,
                true
        ));

        templates.add(buildTemplate(
                "TPL-5001",
                "奥特曼主题生日视频",
                "酷炫动感风，适合男孩主题",
                "hero",
                "奥特曼",
                "blessing",
                "祝福视频",
                "动感",
                "奥特曼IP风格模板",
                new BigDecimal("18.90"),
                "#3550f3",
                "#0f1d67",
                60,
                true
        ));

        templateRepository.saveAll(templates);
    }

    private VideoTemplate buildTemplate(String code,
                                        String name,
                                        String subtitle,
                                        String themeKey,
                                        String themeName,
                                        String categoryKey,
                                        String categoryName,
                                        String tag,
                                        String description,
                                        BigDecimal price,
                                        String colorStart,
                                        String colorEnd,
                                        Integer sortOrder,
                                        Boolean enabled) {
        VideoTemplate template = new VideoTemplate();
        template.setTemplateCode(code);
        template.setName(name);
        template.setSubtitle(subtitle);
        template.setDescription(description);
        template.setThemeKey(themeKey);
        template.setThemeName(themeName);
        template.setCategoryKey(categoryKey);
        template.setCategoryName(categoryName);
        template.setTagName(tag);
        template.setPrice(price);
        template.setColorStart(colorStart);
        template.setColorEnd(colorEnd);
        template.setCoverUrl("");
        template.setPreviewUrl("");
        template.setInventoryCount(99999);
        template.setSalesCount(0L);
        template.setSortOrder(sortOrder);
        template.setEnabled(enabled);
        template.setTemplateType("word");
        return template;
    }

    private void initOrders() {
        if (orderRepository.count() > 0) {
            return;
        }

        List<VideoTemplate> templates = templateRepository.findAll();
        if (templates.isEmpty()) {
            return;
        }

        VideoTemplate first = templates.get(0);

        OrderRecord pendingOrder = new OrderRecord();
        pendingOrder.setOrderNo("177191425186657");
        pendingOrder.setProductId("PRODUCT-ID:9001");
        pendingOrder.setStatus("pending");
        pendingOrder.setAmount(first.getPrice());
        pendingOrder.setCustomerName("李安");
        pendingOrder.setCustomerPhone("13800000000");
        pendingOrder.setRemark("请在现场投屏展示");
        pendingOrder.setTemplate(first);
        orderRepository.save(pendingOrder);

        if (templates.size() > 1) {
            VideoTemplate second = templates.get(1);
            OrderRecord paidOrder = new OrderRecord();
            paidOrder.setOrderNo("177191425186658");
            paidOrder.setProductId("PRODUCT-ID:3761");
            paidOrder.setStatus("paid");
            paidOrder.setAmount(second.getPrice());
            paidOrder.setCustomerName("张琳");
            paidOrder.setCustomerPhone("13900000000");
            paidOrder.setRemark("已确认时间");
            paidOrder.setPaidAt(LocalDateTime.now().minusDays(1));
            paidOrder.setTemplate(second);
            orderRepository.save(paidOrder);

            second.setSalesCount(1L);
            templateRepository.save(second);
        }
    }

    private void initAdminMenus() {
        if (adminMenuRepository.count() > 0) {
            return;
        }

        List<AdminMenu> menus = new ArrayList<>();
        menus.add(menu(0L, "仪表盘", "CATALOG", "/dashboard", "dashboard", "", "chart", 1));
        menus.add(menu(0L, "系统管理", "CATALOG", "/system", "system", "", "settings", 2));
        menus.add(menu(0L, "商城管理", "CATALOG", "/mall", "mall", "", "shop", 3));
        menus.add(menu(0L, "数据统计", "CATALOG", "/statistics", "statistics", "", "bar", 4));

        adminMenuRepository.saveAll(menus);

        AdminMenu dashboard = menus.get(0);
        AdminMenu system = menus.get(1);
        AdminMenu mall = menus.get(2);
        AdminMenu statistics = menus.get(3);

        List<AdminMenu> children = new ArrayList<>();
        children.add(menu(dashboard.getId(), "数据概览", "MENU", "/dashboard/overview", "DashboardOverview", "dashboard:view", "dot", 1));
        children.add(menu(system.getId(), "用户管理", "MENU", "/system/users", "SystemUsers", "sys:user:view", "user", 1));
        children.add(menu(system.getId(), "角色管理", "MENU", "/system/roles", "SystemRoles", "sys:role:view", "team", 2));
        children.add(menu(system.getId(), "菜单管理", "MENU", "/system/menus", "SystemMenus", "sys:menu:view", "menu", 3));
        children.add(menu(mall.getId(), "模板管理", "MENU", "/mall/templates", "MallTemplates", "mall:template:view", "video", 1));
        children.add(menu(mall.getId(), "订单管理", "MENU", "/mall/orders", "MallOrders", "mall:order:view", "order", 2));
        children.add(menu(statistics.getId(), "销售统计", "MENU", "/statistics/sales", "StatsSales", "stats:sales:view", "line", 1));

        adminMenuRepository.saveAll(children);
    }

    private AdminMenu menu(Long parentId,
                           String name,
                           String type,
                           String route,
                           String component,
                           String permission,
                           String icon,
                           int sort) {
        AdminMenu menu = new AdminMenu();
        menu.setParentId(parentId);
        menu.setMenuName(name);
        menu.setMenuType(type);
        menu.setRoutePath(route);
        menu.setComponentPath(component);
        menu.setPermissionKey(permission);
        menu.setIcon(icon);
        menu.setSortOrder(sort);
        menu.setVisible(true);
        menu.setStatus(1);
        return menu;
    }

    private void initAdminRoles() {
        if (adminRoleRepository.count() > 0) {
            return;
        }

        List<AdminMenu> menus = adminMenuRepository.findAllByOrderBySortOrderAscIdAsc();

        AdminRole superAdmin = new AdminRole();
        superAdmin.setRoleCode("SUPER_ADMIN");
        superAdmin.setRoleName("超级管理员");
        superAdmin.setDescription("拥有后台全部权限");
        superAdmin.setStatus(1);
        superAdmin.setMenus(new LinkedHashSet<>(menus));

        AdminRole operator = new AdminRole();
        operator.setRoleCode("OPERATOR");
        operator.setRoleName("运营人员");
        operator.setDescription("可管理模板、订单与数据看板");
        operator.setStatus(1);

        Set<AdminMenu> operatorMenus = new LinkedHashSet<>();
        for (AdminMenu menu : menus) {
            if ("/mall/templates".equals(menu.getRoutePath()) ||
                    "/mall/orders".equals(menu.getRoutePath()) ||
                    "/statistics/sales".equals(menu.getRoutePath()) ||
                    "/dashboard/overview".equals(menu.getRoutePath()) ||
                    "/mall".equals(menu.getRoutePath()) ||
                    "/statistics".equals(menu.getRoutePath()) ||
                    "/dashboard".equals(menu.getRoutePath())) {
                operatorMenus.add(menu);
            }
        }
        operator.setMenus(operatorMenus);

        adminRoleRepository.save(superAdmin);
        adminRoleRepository.save(operator);
    }

    private void initAdminUsers() {
        if (adminUserRepository.count() > 0) {
            return;
        }

        AdminRole superAdminRole = adminRoleRepository.findByRoleCode("SUPER_ADMIN")
                .orElse(null);
        AdminRole operatorRole = adminRoleRepository.findByRoleCode("OPERATOR")
                .orElse(null);

        AdminUser admin = new AdminUser();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123456"));
        admin.setNickname("超级管理员");
        admin.setEmail("admin@example.com");
        admin.setPhone("18800000001");
        admin.setStatus(1);
        if (superAdminRole != null) {
            Set<AdminRole> roles = new LinkedHashSet<>();
            roles.add(superAdminRole);
            admin.setRoles(roles);
        }

        adminUserRepository.save(admin);

        AdminUser operator = new AdminUser();
        operator.setUsername("operator");
        operator.setPasswordHash(passwordEncoder.encode("Operator@123456"));
        operator.setNickname("运营人员");
        operator.setEmail("operator@example.com");
        operator.setPhone("18800000002");
        operator.setStatus(1);
        if (operatorRole != null) {
            Set<AdminRole> roles = new LinkedHashSet<>();
            roles.add(operatorRole);
            operator.setRoles(roles);
        }
        adminUserRepository.save(operator);
    }
}