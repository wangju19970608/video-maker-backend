CREATE DATABASE IF NOT EXISTS birthday_video_maker DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE birthday_video_maker;

CREATE TABLE IF NOT EXISTS video_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    subtitle VARCHAR(255) NULL,
    description VARCHAR(500) NULL,
    theme_key VARCHAR(32) NOT NULL,
    theme_name VARCHAR(32) NOT NULL,
    category_key VARCHAR(32) NOT NULL,
    category_name VARCHAR(32) NOT NULL,
    tag_name VARCHAR(32) NULL,
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    color_start VARCHAR(16) NULL,
    color_end VARCHAR(16) NULL,
    cover_url VARCHAR(255) NULL,
    preview_url VARCHAR(255) NULL,
    inventory_count INT NOT NULL DEFAULT 99999,
    sales_count BIGINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_template_theme (theme_key),
    KEY idx_template_category (category_key),
    KEY idx_template_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(40) NOT NULL UNIQUE,
    product_id VARCHAR(40) NOT NULL,
    status VARCHAR(16) NOT NULL,
    amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    customer_name VARCHAR(64) NULL,
    customer_phone VARCHAR(32) NULL,
    remark VARCHAR(255) NULL,
    paid_at DATETIME NULL,
    template_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_template FOREIGN KEY (template_id) REFERENCES video_template(id),
    KEY idx_order_status (status),
    KEY idx_order_created_at (created_at),
    KEY idx_order_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    email VARCHAR(128) NULL,
    phone VARCHAR(32) NULL,
    status INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_admin_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(64) NOT NULL UNIQUE,
    role_name VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    status INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_admin_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NOT NULL DEFAULT 0,
    menu_name VARCHAR(64) NOT NULL,
    menu_type VARCHAR(16) NOT NULL,
    route_path VARCHAR(128) NULL,
    component_path VARCHAR(128) NULL,
    permission_key VARCHAR(128) NULL,
    icon VARCHAR(64) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    visible TINYINT(1) NOT NULL DEFAULT 1,
    status INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_admin_menu_parent (parent_id),
    KEY idx_admin_menu_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_admin_user_role_user FOREIGN KEY (user_id) REFERENCES admin_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_admin_user_role_role FOREIGN KEY (role_id) REFERENCES admin_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id),
    CONSTRAINT fk_admin_role_menu_role FOREIGN KEY (role_id) REFERENCES admin_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_admin_role_menu_menu FOREIGN KEY (menu_id) REFERENCES admin_menu(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS template_config (
                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               template_id BIGINT NOT NULL,
                                               overlay_rules TEXT,
                                               form_fields TEXT,
                                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                               UNIQUE KEY uk_template_id (template_id),
    FOREIGN KEY (template_id) REFERENCES video_template(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板配置表（叠加规则+表单字段）';

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    openid VARCHAR(64) NOT NULL UNIQUE COMMENT '微信openid',
    unionid VARCHAR(64) NULL COMMENT '微信unionid',
    nickname VARCHAR(64) NULL COMMENT '用户昵称',
    avatar_url VARCHAR(512) NULL COMMENT '头像URL',
    phone VARCHAR(32) NULL COMMENT '手机号',
    session_key VARCHAR(128) NULL COMMENT '微信session_key',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    last_login_at DATETIME NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_sys_user_openid (openid),
    KEY idx_sys_user_unionid (unionid),
    KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序用户表';
