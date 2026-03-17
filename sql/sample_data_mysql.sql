USE birthday_video_maker;

INSERT INTO video_template
(template_code, name, subtitle, description, theme_key, theme_name, category_key, category_name, tag_name, price, color_start, color_end, cover_url, preview_url, inventory_count, sales_count, sort_order, enabled)
VALUES
('TPL-9001', '生日派对大屏祝福', '适合宴会投屏，动态粒子', '生日会现场投屏模板', 'screen', '生日投屏', 'tv', '电视投屏图', '高清', 15.90, '#ff4b54', '#9a031e', '/api/assets/templates/1.png', 'template/1.docx', 99999, 0, 5, 1),
('TPL-3761', '乔迁大屏祝福', '红金国风，适配宴会投屏', '适配大屏的乔迁投屏模板', 'housewarming', '乔迁', 'tv', '电视投屏图', '精选', 19.90, '#c90b18', '#81000f', '', '', 99999, 0, 10, 1),
('TPL-3762', '乔迁邀请函', '竖屏邀请函模板，支持多图', '用于乔迁宴会邀请场景', 'housewarming', '乔迁', 'invitation', '邀请函', '热卖', 19.90, '#a40b14', '#5f000a', '', '', 99999, 1, 20, 1),
('TPL-4102', '生日投屏烟花祝福', '适合宴会投屏，动效焰火', '生日会现场投屏祝福模板', 'screen', '生日投屏', 'tv', '电视投屏图', '高清', 15.90, '#ff4b54', '#9a031e', '', '', 99999, 0, 30, 1),
('TPL-4518', '升学典礼荣誉时刻', '学业成长主题，支持多图轮播', '升学季祝福短视频模板', 'study', '升学', 'blessing', '祝福视频', '新品', 16.90, '#2f6cd6', '#10366f', '', '', 99999, 0, 40, 1),
('TPL-4820', '温馨生日祝福视频', '家庭相册版，支持背景音乐', '家庭相册类型生日模板', 'birthday', '生日祝福', 'blessing', '祝福视频', '热门', 12.90, '#ff8e53', '#a7135f', '', '', 99999, 0, 50, 1),
('TPL-5001', '奥特曼主题生日视频', '酷炫动感风，适合男孩主题', '奥特曼IP风格模板', 'hero', '奥特曼', 'blessing', '祝福视频', '动感', 18.90, '#3550f3', '#0f1d67', '', '', 99999, 0, 60, 1)
ON DUPLICATE KEY UPDATE
name = VALUES(name),
subtitle = VALUES(subtitle),
description = VALUES(description),
price = VALUES(price),
color_start = VALUES(color_start),
color_end = VALUES(color_end),
cover_url = VALUES(cover_url),
preview_url = VALUES(preview_url),
enabled = VALUES(enabled),
updated_at = CURRENT_TIMESTAMP;

INSERT INTO order_info
(order_no, product_id, status, amount, customer_name, customer_phone, remark, paid_at, template_id)
SELECT '177191425186657', 'PRODUCT-ID:9001', 'pending', 15.90, '李安', '13800000000', '请在现场投屏展示', NULL, id
FROM video_template WHERE template_code = 'TPL-9001'
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

INSERT INTO order_info
(order_no, product_id, status, amount, customer_name, customer_phone, remark, paid_at, template_id)
SELECT '177191425186658', 'PRODUCT-ID:3761', 'paid', 19.90, '张琳', '13900000000', '已确认时间', DATE_SUB(NOW(), INTERVAL 1 DAY), id
FROM video_template WHERE template_code = 'TPL-3761'
ON DUPLICATE KEY UPDATE status = VALUES(status), paid_at = VALUES(paid_at), updated_at = CURRENT_TIMESTAMP;

INSERT INTO admin_menu (id, parent_id, menu_name, menu_type, route_path, component_path, permission_key, icon, sort_order, visible, status)
VALUES
(1, 0, '仪表盘', 'CATALOG', '/dashboard', 'dashboard', '', 'chart', 1, 1, 1),
(2, 0, '系统管理', 'CATALOG', '/system', 'system', '', 'settings', 2, 1, 1),
(3, 0, '商城管理', 'CATALOG', '/mall', 'mall', '', 'shop', 3, 1, 1),
(4, 0, '数据统计', 'CATALOG', '/statistics', 'statistics', '', 'bar', 4, 1, 1),
(11, 1, '数据概览', 'MENU', '/dashboard/overview', 'DashboardOverview', 'dashboard:view', 'dot', 1, 1, 1),
(21, 2, '用户管理', 'MENU', '/system/users', 'SystemUsers', 'sys:user:view', 'user', 1, 1, 1),
(22, 2, '角色管理', 'MENU', '/system/roles', 'SystemRoles', 'sys:role:view', 'team', 2, 1, 1),
(23, 2, '菜单管理', 'MENU', '/system/menus', 'SystemMenus', 'sys:menu:view', 'menu', 3, 1, 1),
(31, 3, '模板管理', 'MENU', '/mall/templates', 'MallTemplates', 'mall:template:view', 'video', 1, 1, 1),
(32, 3, '订单管理', 'MENU', '/mall/orders', 'MallOrders', 'mall:order:view', 'order', 2, 1, 1),
(41, 4, '销售统计', 'MENU', '/statistics/sales', 'StatsSales', 'stats:sales:view', 'line', 1, 1, 1)
ON DUPLICATE KEY UPDATE
menu_name = VALUES(menu_name),
route_path = VALUES(route_path),
component_path = VALUES(component_path),
permission_key = VALUES(permission_key),
updated_at = CURRENT_TIMESTAMP;

INSERT INTO admin_role (id, role_code, role_name, description, status)
VALUES
(1, 'SUPER_ADMIN', '超级管理员', '拥有后台全部权限', 1),
(2, 'OPERATOR', '运营人员', '可管理模板、订单与数据看板', 1)
ON DUPLICATE KEY UPDATE
role_name = VALUES(role_name),
description = VALUES(description),
updated_at = CURRENT_TIMESTAMP;

INSERT INTO admin_user (id, username, password_hash, nickname, email, phone, status)
VALUES
(1, 'admin', 'Admin@123456', '超级管理员', 'admin@example.com', '18800000001', 1),
(2, 'operator', 'Operator@123456', '运营人员', 'operator@example.com', '18800000002', 1)
ON DUPLICATE KEY UPDATE
nickname = VALUES(nickname),
email = VALUES(email),
phone = VALUES(phone),
status = VALUES(status),
updated_at = CURRENT_TIMESTAMP;

INSERT IGNORE INTO admin_user_role (user_id, role_id)
VALUES
(1, 1),
(2, 2);

INSERT IGNORE INTO admin_role_menu (role_id, menu_id)
VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 11), (1, 21), (1, 22), (1, 23), (1, 31), (1, 32), (1, 41),
(2, 1), (2, 3), (2, 4), (2, 11), (2, 31), (2, 32), (2, 41);