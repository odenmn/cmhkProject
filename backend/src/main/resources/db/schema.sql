CREATE DATABASE IF NOT EXISTS cmhk
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE cmhk;

CREATE TABLE IF NOT EXISTS business_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO business_type (code, name, description, sort_order, enabled)
VALUES
    ('MOBILE_PLAN', '移动套餐办理', '查询、推荐和办理 CMHK 移动通信套餐', 10, 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    enabled = VALUES(enabled);

DELETE FROM business_type
WHERE code IN ('BROADBAND', 'VALUE_ADDED');

CREATE TABLE IF NOT EXISTS mobile_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_code VARCHAR(64) NOT NULL UNIQUE,
    plan_name VARCHAR(128) NOT NULL,
    monthly_fee DECIMAL(10, 2) NOT NULL,
    data_quota VARCHAR(64) NOT NULL,
    voice_quota VARCHAR(64) NOT NULL,
    contract_period VARCHAR(64) NOT NULL,
    description VARCHAR(512),
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO mobile_plan (
    plan_code,
    plan_name,
    monthly_fee,
    data_quota,
    voice_quota,
    contract_period,
    description,
    sort_order,
    enabled
)
VALUES
    ('CMHK_5G_128', '5G 畅享 128 套餐', 128.00, '30GB 本地数据', '1000 分钟本地通话', '12 个月', '适合日常通讯、视频和社交使用。', 10, 1),
    ('CMHK_5G_198', '5G 畅享 198 套餐', 198.00, '80GB 本地数据', '2000 分钟本地通话', '12 个月', '适合高频上网、热点共享和商务使用。', 20, 1),
    ('CMHK_5G_298', '5G 尊享 298 套餐', 298.00, '150GB 本地数据', '无限本地通话', '24 个月', '适合重度数据用户和家庭共享场景。', 30, 1)
ON DUPLICATE KEY UPDATE
    plan_name = VALUES(plan_name),
    monthly_fee = VALUES(monthly_fee),
    data_quota = VALUES(data_quota),
    voice_quota = VALUES(voice_quota),
    contract_period = VALUES(contract_period),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    enabled = VALUES(enabled);

CREATE TABLE IF NOT EXISTS mobile_plan_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    plan_code VARCHAR(64) NOT NULL,
    plan_name VARCHAR(128) NOT NULL,
    monthly_fee DECIMAL(10, 2) NOT NULL,
    customer_name VARCHAR(64),
    contact_phone VARCHAR(32) NOT NULL,
    remark VARCHAR(512),
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_mobile_plan_order_plan_code (plan_code),
    INDEX idx_mobile_plan_order_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
