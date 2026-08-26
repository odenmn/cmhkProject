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
    plan_type VARCHAR(64) NOT NULL,
    monthly_fee DECIMAL(10, 2) NOT NULL,
    channel_price_text VARCHAR(64) NOT NULL,
    effective_monthly_fee DECIMAL(10, 2),
    effective_price_text VARCHAR(64),
    official_monthly_fee DECIMAL(10, 2),
    official_price_text VARCHAR(64),
    data_quota VARCHAR(128) NOT NULL,
    voice_quota VARCHAR(128),
    roaming_benefit VARCHAR(128),
    contract_period VARCHAR(64),
    promotion_end_date DATE,
    source_version VARCHAR(32),
    discount_formula VARCHAR(512),
    description VARCHAR(512),
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_column_if_missing;

DELIMITER //
CREATE PROCEDURE add_column_if_missing(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64),
    IN column_definition VARCHAR(512),
    IN after_column VARCHAR(64)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND column_name = target_column
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE ', target_table,
            ' ADD COLUMN ', target_column, ' ', column_definition,
            IF(after_column IS NULL OR after_column = '', '', CONCAT(' AFTER ', after_column))
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    phone VARCHAR(32),
    email VARCHAR(128),
    role_code VARCHAR(32) NOT NULL DEFAULT 'ADMIN',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    last_login_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_admin_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CALL add_column_if_missing('mobile_plan', 'plan_type', 'VARCHAR(64) NOT NULL DEFAULT ''移动套餐''', 'plan_name');
CALL add_column_if_missing('mobile_plan', 'channel_price_text', 'VARCHAR(64) NOT NULL DEFAULT ''''', 'monthly_fee');
CALL add_column_if_missing('mobile_plan', 'effective_monthly_fee', 'DECIMAL(10, 2) NULL', 'channel_price_text');
CALL add_column_if_missing('mobile_plan', 'effective_price_text', 'VARCHAR(64) NULL', 'effective_monthly_fee');
CALL add_column_if_missing('mobile_plan', 'official_monthly_fee', 'DECIMAL(10, 2) NULL', 'effective_price_text');
CALL add_column_if_missing('mobile_plan', 'official_price_text', 'VARCHAR(64) NULL', 'official_monthly_fee');
CALL add_column_if_missing('mobile_plan', 'roaming_benefit', 'VARCHAR(128) NULL', 'voice_quota');
CALL add_column_if_missing('mobile_plan', 'promotion_end_date', 'DATE NULL', 'contract_period');
CALL add_column_if_missing('mobile_plan', 'source_version', 'VARCHAR(32) NULL', 'promotion_end_date');
CALL add_column_if_missing('mobile_plan', 'discount_formula', 'VARCHAR(512) NULL', 'source_version');

INSERT INTO mobile_plan (
    plan_code,
    plan_name,
    plan_type,
    monthly_fee,
    channel_price_text,
    effective_monthly_fee,
    effective_price_text,
    official_monthly_fee,
    official_price_text,
    data_quota,
    voice_quota,
    roaming_benefit,
    contract_period,
    promotion_end_date,
    source_version,
    discount_formula,
    description,
    sort_order,
    enabled
)
VALUES
    ('ONE_CARD_TWO_PLACE_5G_50GB_24M', '一卡两地 5G 50GB', '一卡两地', 179.00, 'HK$179/月', NULL, NULL, 179.00, 'HK$179/月', '50GB 两地共用', '香港本地无限通话', '内地 200 分钟/月', '24个月', '2026-07-31', '202607', NULL, '适合需要香港与内地两地流量、通话权益的客户。', 10, 1),
    ('HK_LOCAL_5G_100GB_24M', '香港本地 5G 100GB', '香港本地', 149.00, 'HK$149/月', NULL, NULL, 149.00, 'HK$149/月', '香港本地 100GB', '香港本地无限通话', '2GB', '24个月', '2026-07-31', '202607', NULL, '适合主要在香港本地使用大流量的客户。', 20, 1),
    ('STUDENT_SLASH_30GB_24M', '学生 Slash 30GB', '学生套餐', 98.00, 'HK$98/月', 62.00, '约HK$62/月', 98.00, 'HK$98/月', '30GB', '香港本地无限通话', '赠3GB', '24个月', '2026-07-31', '202607', '(HK$98 x 24个月 - HK$600话费券 - HK$260渠道补贴) / 24个月', '留学生上台优惠，24 个月折实月费更低。', 30, 1),
    ('STUDENT_SLASH_30GB_12M', '学生 Slash 30GB', '学生套餐', 118.00, 'HK$118/月', 71.00, '约HK$71/月', 118.00, 'HK$118/月', '30GB', '香港本地无限通话', '赠3GB', '12个月', '2026-07-31', '202607', '(HK$118 x 12个月 - HK$400话费券 - HK$168渠道补贴) / 12个月', '留学生上台优惠，12 个月合约更灵活。', 40, 1),
    ('STUDENT_SLASH_50GB_24M', '学生 Slash 50GB', '学生套餐', 138.00, 'HK$138/月', 102.00, '约HK$102/月', 138.00, 'HK$138/月', '50GB + 限时额外50GB，最高100GB 香港本地数据', '香港本地无限通话', '中国内地及澳门数据4GB + 限时额外2GB，最高6GB', '24个月', '2026-07-31', '202607', '(HK$138 x 24个月 - HK$600电子缴费券 - HK$260渠道补贴) / 24个月', '秋季校园优惠主推款，适合学生长期使用。', 50, 1),
    ('STUDENT_SLASH_50GB_12M', '学生 Slash 50GB', '学生套餐', 158.00, 'HK$158/月', 105.00, '约HK$105/月', 158.00, 'HK$158/月', '50GB + 限时额外50GB，最高100GB 香港本地数据', '香港本地无限通话', '中国内地及澳门数据4GB + 限时额外2GB，最高6GB', '12个月', '2026-07-31', '202607', '(HK$158 x 12个月 - HK$400电子缴费券 - HK$240渠道补贴) / 12个月', '秋季校园优惠 12 个月方案，适合短期留学客户。', 60, 1),
    ('ONE_CARD_TWO_PLACE_5G_100GB_24M', '一卡两地 5G 100GB', '一卡两地', 249.00, 'HK$249/月', NULL, NULL, 249.00, 'HK$249/月', '100GB 两地共用', '香港本地无限通话', '内地500分钟/月', '24个月', '2026-07-31', '202607', NULL, '适合两地高频使用的中高流量客户。', 70, 1),
    ('ONE_CARD_TWO_PLACE_5G_200GB_24M', '一卡两地 5G 200GB', '一卡两地', 399.00, 'HK$399/月', NULL, NULL, 399.00, 'HK$399/月', '200GB 两地共用', '香港本地无限通话', '内地500分钟/月', '24个月', '2026-07-31', '202607', NULL, '适合两地重度数据使用客户。', 80, 1),
    ('STAFF_ONE_CARD_TWO_PLACE_5G_50GB_24M', 'company staff 一卡两地 5G 50GB', '员工一卡两地', 159.00, 'HK$159/月', NULL, NULL, 159.00, 'HK$159/月', '50GB 两地共用', '香港本地无限通话', '内地200 分钟/月', '24个月', '2026-07-31', '202607', NULL, '员工渠道一卡两地 50GB 优惠方案。', 90, 1),
    ('STAFF_ONE_CARD_TWO_PLACE_5G_100GB_24M', 'company staff 一卡两地 5G 100GB', '员工一卡两地', 229.00, 'HK$229/月', NULL, NULL, 229.00, 'HK$229/月', '100GB 两地共用', '香港本地无限通话', '内地500分钟/月', '24个月', '2026-07-31', '202607', NULL, '员工渠道一卡两地 100GB 优惠方案。', 100, 1),
    ('ONE_CARD_THREE_PLACE_5G_30GB_24M', '一卡三地 5G 30GB', '一卡三地', 199.00, 'HK$199/月', NULL, NULL, 199.00, 'HK$199/月', '30GB 三地共用', '香港本地无限通话', '漫游通话 200 分钟/月', '24个月', '2026-07-31', '202607', NULL, '适合香港、内地及海外三地通信用量客户。', 110, 1),
    ('ONE_CARD_THREE_PLACE_5G_60GB_24M', '一卡三地 5G 60GB', '一卡三地', 239.00, 'HK$239/月', NULL, NULL, 239.00, 'HK$239/月', '60GB 三地共用', '香港本地无限通话', '漫游通话200 分钟/月', '24个月', '2026-07-31', '202607', NULL, '适合三地通信用量较高客户。', 120, 1),
    ('STAFF_ONE_CARD_THREE_PLACE_5G_60GB_24M', 'company staff 一卡三地 5G 60GB', '员工一卡三地', 219.00, 'HK$219/月', NULL, NULL, 239.00, 'HK$239/月', '60GB 三地共用', '香港本地无限通话', '漫游通话200 分钟/月', '24个月', '2026-07-31', '202607', NULL, '员工渠道一卡三地 60GB 优惠方案。', 130, 1),
    ('STAFF_ONE_CARD_THREE_PLACE_5G_30GB_24M', 'company staff 一卡三地 5G 30GB', '员工一卡三地', 179.00, 'HK$179/月', NULL, NULL, 199.00, 'HK$199/月', '30GB 三地共用', '香港本地无限通话', '漫游通话200 分钟/月', '24个月', '2026-07-31', '202607', NULL, '员工渠道一卡三地 30GB 优惠方案。', 140, 1)
ON DUPLICATE KEY UPDATE
    plan_name = VALUES(plan_name),
    plan_type = VALUES(plan_type),
    monthly_fee = VALUES(monthly_fee),
    channel_price_text = VALUES(channel_price_text),
    effective_monthly_fee = VALUES(effective_monthly_fee),
    effective_price_text = VALUES(effective_price_text),
    official_monthly_fee = VALUES(official_monthly_fee),
    official_price_text = VALUES(official_price_text),
    data_quota = VALUES(data_quota),
    voice_quota = VALUES(voice_quota),
    roaming_benefit = VALUES(roaming_benefit),
    contract_period = VALUES(contract_period),
    promotion_end_date = VALUES(promotion_end_date),
    source_version = VALUES(source_version),
    discount_formula = VALUES(discount_formula),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    enabled = VALUES(enabled);

UPDATE mobile_plan
SET enabled = 0
WHERE plan_code IN ('CMHK_5G_128', 'CMHK_5G_198', 'CMHK_5G_298');

CREATE TABLE IF NOT EXISTS mobile_plan_offer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_code VARCHAR(64) NOT NULL,
    offer_type VARCHAR(64) NOT NULL,
    offer_name VARCHAR(128) NOT NULL,
    offer_value VARCHAR(256) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_mobile_plan_offer (plan_code, offer_type, offer_name),
    INDEX idx_mobile_plan_offer_plan_code (plan_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO mobile_plan_offer (plan_code, offer_type, offer_name, offer_value, sort_order, enabled)
VALUES
    ('STUDENT_SLASH_30GB_24M', 'EXTRA_DATA', '内地及澳门数据', '赠3GB', 10, 1),
    ('STUDENT_SLASH_30GB_24M', 'VOICE', '香港通话', '无限通话', 20, 1),
    ('STUDENT_SLASH_30GB_24M', 'VOICE', '内地通话', '50分钟免费通话', 30, 1),
    ('STUDENT_SLASH_30GB_24M', 'FEE_WAIVER', '学生新上台', '免行政费', 40, 1),
    ('STUDENT_SLASH_30GB_24M', 'POINTS', '官方积分', '30,000 MyLink', 50, 1),
    ('STUDENT_SLASH_30GB_24M', 'POINTS', '推荐积分', '30,000 MyLink', 60, 1),
    ('STUDENT_SLASH_30GB_24M', 'POINTS', '积分合计', '共60,000分，可抵HK$600话费券', 70, 1),
    ('STUDENT_SLASH_30GB_24M', 'SUBSIDY', '渠道额外补贴', 'HK$260', 80, 1),
    ('STUDENT_SLASH_30GB_24M', 'SOCIAL_DATA', '社交及娱乐数据组合', 'WhatsApp、WeChat、LINE、Telegram、Zoom、Teams、YouTube、Netflix、Apple TV、Facebook、TikTok、Instagram 等', 90, 1),
    ('STUDENT_SLASH_30GB_12M', 'EXTRA_DATA', '内地及澳门数据', '赠3GB', 10, 1),
    ('STUDENT_SLASH_30GB_12M', 'VOICE', '香港通话', '无限通话', 20, 1),
    ('STUDENT_SLASH_30GB_12M', 'VOICE', '内地通话', '50分钟免费通话', 30, 1),
    ('STUDENT_SLASH_30GB_12M', 'FEE_WAIVER', '学生新上台', '免行政费', 40, 1),
    ('STUDENT_SLASH_30GB_12M', 'POINTS', '官方积分', '20,000 MyLink', 50, 1),
    ('STUDENT_SLASH_30GB_12M', 'POINTS', '推荐积分', '20,000 MyLink', 60, 1),
    ('STUDENT_SLASH_30GB_12M', 'POINTS', '积分合计', '共40,000分，可抵HK$400话费券', 70, 1),
    ('STUDENT_SLASH_30GB_12M', 'SUBSIDY', '渠道额外补贴', 'HK$168', 80, 1),
    ('STUDENT_SLASH_30GB_12M', 'SOCIAL_DATA', '社交及娱乐数据组合', 'WhatsApp、WeChat、LINE、Telegram、Zoom、Teams、YouTube、Netflix、Apple TV、Facebook、TikTok、Instagram 等', 90, 1),
    ('STUDENT_SLASH_50GB_24M', 'EXTRA_DATA', '香港本地数据', '50GB + 限时额外50GB，最高100GB', 10, 1),
    ('STUDENT_SLASH_50GB_24M', 'EXTRA_DATA', '中国内地及澳门数据', '4GB + 限时额外2GB，最高6GB', 20, 1),
    ('STUDENT_SLASH_50GB_24M', 'FEE_WAIVER', '学生新上台', '免行政费', 30, 1),
    ('STUDENT_SLASH_50GB_24M', 'POINTS', 'MyLink积分', '30,000分', 40, 1),
    ('STUDENT_SLASH_50GB_24M', 'POINTS', '推荐人号码积分', '30,000分', 50, 1),
    ('STUDENT_SLASH_50GB_24M', 'POINTS', '积分合计', '60,000分，可抵HK$600电子缴费券', 60, 1),
    ('STUDENT_SLASH_50GB_24M', 'SUBSIDY', '渠道额外补贴', 'HK$260', 70, 1),
    ('STUDENT_SLASH_50GB_24M', 'SUBSIDY', '购机补贴', 'HK$600', 80, 1),
    ('STUDENT_SLASH_50GB_24M', 'SCENE', '适用场景', '日常上课、社交、导航、睇片及两地往返使用', 90, 1),
    ('STUDENT_SLASH_50GB_12M', 'EXTRA_DATA', '香港本地数据', '50GB + 限时额外50GB，最高100GB', 10, 1),
    ('STUDENT_SLASH_50GB_12M', 'EXTRA_DATA', '中国内地及澳门数据', '4GB + 限时额外2GB，最高6GB', 20, 1),
    ('STUDENT_SLASH_50GB_12M', 'FEE_WAIVER', '学生新上台', '免行政费', 30, 1),
    ('STUDENT_SLASH_50GB_12M', 'POINTS', 'MyLink积分', '20,000分', 40, 1),
    ('STUDENT_SLASH_50GB_12M', 'POINTS', '推荐人号码积分', '20,000分', 50, 1),
    ('STUDENT_SLASH_50GB_12M', 'POINTS', '积分合计', '40,000分，可抵HK$400电子缴费券', 60, 1),
    ('STUDENT_SLASH_50GB_12M', 'SUBSIDY', '渠道额外补贴', 'HK$240', 70, 1),
    ('STUDENT_SLASH_50GB_12M', 'SCENE', '适用场景', '日常上课、社交、导航、睇片及两地往返使用', 80, 1)
ON DUPLICATE KEY UPDATE
    offer_value = VALUES(offer_value),
    sort_order = VALUES(sort_order),
    enabled = VALUES(enabled);

CREATE TABLE IF NOT EXISTS mobile_plan_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    plan_id BIGINT,
    plan_code VARCHAR(64) NOT NULL,
    plan_name VARCHAR(128) NOT NULL,
    plan_type VARCHAR(64),
    monthly_fee DECIMAL(10, 2) NOT NULL,
    channel_price_text VARCHAR(64),
    effective_monthly_fee DECIMAL(10, 2),
    effective_price_text VARCHAR(64),
    official_monthly_fee DECIMAL(10, 2),
    official_price_text VARCHAR(64),
    data_quota VARCHAR(128),
    voice_quota VARCHAR(128),
    roaming_benefit VARCHAR(128),
    contract_period VARCHAR(64),
    promotion_end_date DATE,
    discount_formula VARCHAR(512),
    customer_name VARCHAR(64),
    contact_phone VARCHAR(32) NOT NULL,
    customer_identity TINYINT NOT NULL DEFAULT 0,
    has_offer TINYINT NOT NULL DEFAULT 0,
    has_pass_or_hkid TINYINT NOT NULL DEFAULT 0,
    expected_start_date DATE,
    id_type VARCHAR(32),
    id_no VARCHAR(64),
    referrer_phone VARCHAR(32),
    preferred_contact_time VARCHAR(128),
    remark VARCHAR(512),
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_mobile_plan_order_plan_id (plan_id),
    INDEX idx_mobile_plan_order_customer_id (customer_id),
    INDEX idx_mobile_plan_order_plan_code (plan_code),
    INDEX idx_mobile_plan_order_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CALL add_column_if_missing('mobile_plan_order', 'plan_id', 'BIGINT NULL', 'order_no');
CALL add_column_if_missing('mobile_plan_order', 'customer_id', 'BIGINT NULL', 'order_no');
CALL add_column_if_missing('mobile_plan_order', 'plan_type', 'VARCHAR(64) NULL', 'plan_name');
CALL add_column_if_missing('mobile_plan_order', 'channel_price_text', 'VARCHAR(64) NULL', 'monthly_fee');
CALL add_column_if_missing('mobile_plan_order', 'effective_monthly_fee', 'DECIMAL(10, 2) NULL', 'channel_price_text');
CALL add_column_if_missing('mobile_plan_order', 'effective_price_text', 'VARCHAR(64) NULL', 'effective_monthly_fee');
CALL add_column_if_missing('mobile_plan_order', 'official_monthly_fee', 'DECIMAL(10, 2) NULL', 'effective_price_text');
CALL add_column_if_missing('mobile_plan_order', 'official_price_text', 'VARCHAR(64) NULL', 'official_monthly_fee');
CALL add_column_if_missing('mobile_plan_order', 'data_quota', 'VARCHAR(128) NULL', 'official_price_text');
CALL add_column_if_missing('mobile_plan_order', 'voice_quota', 'VARCHAR(128) NULL', 'data_quota');
CALL add_column_if_missing('mobile_plan_order', 'roaming_benefit', 'VARCHAR(128) NULL', 'voice_quota');
CALL add_column_if_missing('mobile_plan_order', 'contract_period', 'VARCHAR(64) NULL', 'roaming_benefit');
CALL add_column_if_missing('mobile_plan_order', 'promotion_end_date', 'DATE NULL', 'contract_period');
CALL add_column_if_missing('mobile_plan_order', 'discount_formula', 'VARCHAR(512) NULL', 'promotion_end_date');
CALL add_column_if_missing('mobile_plan_order', 'customer_identity', 'TINYINT NOT NULL DEFAULT 0', 'contact_phone');
CALL add_column_if_missing('mobile_plan_order', 'has_offer', 'TINYINT NOT NULL DEFAULT 0', 'customer_identity');
CALL add_column_if_missing('mobile_plan_order', 'has_pass_or_hkid', 'TINYINT NOT NULL DEFAULT 0', 'has_offer');
CALL add_column_if_missing('mobile_plan_order', 'expected_start_date', 'DATE NULL', 'has_pass_or_hkid');
CALL add_column_if_missing('mobile_plan_order', 'id_type', 'VARCHAR(32) NULL', 'expected_start_date');
CALL add_column_if_missing('mobile_plan_order', 'id_no', 'VARCHAR(64) NULL', 'id_type');
CALL add_column_if_missing('mobile_plan_order', 'referrer_phone', 'VARCHAR(32) NULL', 'id_no');
CALL add_column_if_missing('mobile_plan_order', 'preferred_contact_time', 'VARCHAR(128) NULL', 'referrer_phone');

DROP PROCEDURE IF EXISTS drop_column_if_exists;

DELIMITER //
CREATE PROCEDURE drop_column_if_exists(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND column_name = target_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', target_table, ' DROP COLUMN ', target_column);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL drop_column_if_exists('mobile_plan_order', 'has_offer_plus_or_hkid');
CALL drop_column_if_exists('mobile_plan_order', 'has_offer_plus');

DROP PROCEDURE IF EXISTS add_index_if_missing;

CREATE TABLE IF NOT EXISTS channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_code VARCHAR(64) NOT NULL UNIQUE,
    channel_name VARCHAR(128) NOT NULL,
    elderly_mode TINYINT NOT NULL DEFAULT 0,
    wechat_service_url VARCHAR(512),
    wechat_qr_code_url VARCHAR(512),
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS channel_entry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_id BIGINT NOT NULL,
    entry_token VARCHAR(128) NOT NULL UNIQUE,
    entry_name VARCHAR(128) NOT NULL,
    expires_at DATETIME,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_channel_entry_channel_id (channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(32) NOT NULL UNIQUE,
    phone_verified_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customer_channel_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL UNIQUE,
    channel_id BIGINT NOT NULL,
    entry_id BIGINT NOT NULL,
    bound_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_customer_channel_binding_channel_id (channel_id),
    INDEX idx_customer_channel_binding_entry_id (entry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS phone_verification_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(32) NOT NULL,
    code_hash CHAR(64) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    expires_at DATETIME NOT NULL,
    used_at DATETIME,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_phone_verification_code_phone_created (phone, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO channel (channel_code, channel_name, elderly_mode, enabled)
VALUES
    ('CMHK_DIRECT', 'CMHK 自营渠道', 0, 1),
    ('CMHK_ELDERLY', 'CMHK 长者关怀渠道', 1, 1)
ON DUPLICATE KEY UPDATE
    channel_name = VALUES(channel_name),
    elderly_mode = VALUES(elderly_mode),
    enabled = VALUES(enabled);

INSERT INTO channel_entry (channel_id, entry_token, entry_name, enabled)
SELECT id, 'DEMO-ENTRY-001', '自营渠道演示入口', 1
FROM channel
WHERE channel_code = 'CMHK_DIRECT'
ON DUPLICATE KEY UPDATE
    entry_name = VALUES(entry_name),
    enabled = VALUES(enabled);

INSERT INTO channel_entry (channel_id, entry_token, entry_name, enabled)
SELECT id, 'ELDERLY-ENTRY-001', '长者关怀演示入口', 1
FROM channel
WHERE channel_code = 'CMHK_ELDERLY'
ON DUPLICATE KEY UPDATE
    entry_name = VALUES(entry_name),
    enabled = VALUES(enabled);

DELIMITER //
CREATE PROCEDURE add_index_if_missing(
    IN target_table VARCHAR(64),
    IN target_index VARCHAR(64),
    IN index_definition VARCHAR(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND index_name = target_index
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', target_table, ' ADD INDEX ', target_index, ' ', index_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_index_if_missing('mobile_plan_order', 'idx_mobile_plan_order_plan_id', '(plan_id)');
CALL add_index_if_missing('mobile_plan_order', 'idx_mobile_plan_order_customer_id', '(customer_id)');

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS drop_column_if_exists;
DROP PROCEDURE IF EXISTS add_index_if_missing;

-- JOINCOM 管理后台 MVP：客户、订单、ICCID、甲方对账和二级渠道结算。
DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64),
    IN column_definition VARCHAR(512),
    IN after_column VARCHAR(64)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = target_table AND column_name = target_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', target_table, ' ADD COLUMN ', target_column, ' ', column_definition,
            IF(after_column IS NULL OR after_column = '', '', CONCAT(' AFTER ', after_column)));
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_column_if_missing('customer', 'name', 'VARCHAR(64) NULL', 'phone_verified_at');
CALL add_column_if_missing('customer', 'contact_method', 'VARCHAR(128) NULL', 'name');
CALL add_column_if_missing('customer', 'customer_type', 'VARCHAR(32) NOT NULL DEFAULT ''DIRECT''', 'contact_method');
CALL add_column_if_missing('customer', 'channel_id', 'BIGINT NULL', 'customer_type');
CALL add_column_if_missing('customer', 'intended_plan', 'VARCHAR(128) NULL', 'channel_id');
CALL add_column_if_missing('customer', 'requirement_summary', 'VARCHAR(512) NULL', 'intended_plan');
CALL add_column_if_missing('customer', 'current_status', 'VARCHAR(32) NOT NULL DEFAULT ''待处理''', 'requirement_summary');

CALL add_column_if_missing('mobile_plan_order', 'umall_order_no', 'VARCHAR(64) NULL', 'status');
CALL add_column_if_missing('mobile_plan_order', 'service_number', 'VARCHAR(32) NULL', 'umall_order_no');
CALL add_column_if_missing('mobile_plan_order', 'activation_status', 'VARCHAR(32) NULL', 'service_number');
CALL add_column_if_missing('mobile_plan_order', 'contract_status', 'VARCHAR(32) NULL', 'activation_status');
CALL add_column_if_missing('mobile_plan_order', 'order_source', 'VARCHAR(32) NOT NULL DEFAULT ''H5''', 'contract_status');
CALL add_column_if_missing('mobile_plan_order', 'reconciliation_status', 'VARCHAR(32) NOT NULL DEFAULT ''待对账''', 'order_source');

CREATE TABLE IF NOT EXISTS iccid_inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    iccid VARCHAR(32) NOT NULL UNIQUE,
    batch_no VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    current_customer_id BIGINT,
    current_order_id BIGINT,
    assigned_at DATETIME,
    used_at DATETIME,
    operator_name VARCHAR(64),
    remark VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_iccid_status (status),
    INDEX idx_iccid_customer (current_customer_id),
    INDEX idx_iccid_order (current_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS iccid_assignment_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    iccid_id BIGINT NOT NULL,
    iccid VARCHAR(32) NOT NULL,
    customer_id BIGINT,
    order_id BIGINT,
    action_type VARCHAR(32) NOT NULL,
    operator_name VARCHAR(64) NOT NULL,
    reason VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_iccid_history_iccid (iccid_id),
    INDEX idx_iccid_history_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cmhk_reconciliation_import (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    file_hash CHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    total_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    unmatched_count INT NOT NULL DEFAULT 0,
    operator_name VARCHAR(64) NOT NULL,
    confirmed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reconciliation_file_hash (file_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cmhk_reconciliation_row (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_id BIGINT NOT NULL,
    source_row_number INT,
    raw_data JSON,
    umall_order_no VARCHAR(64),
    iccid VARCHAR(32),
    phone VARCHAR(32),
    plan_name VARCHAR(128),
    review_status VARCHAR(32),
    supplement_status VARCHAR(128),
    activation_status VARCHAR(32),
    contract_status VARCHAR(32),
    commission_amount DECIMAL(12,2),
    matched_order_id BIGINT,
    match_method VARCHAR(32),
    match_status VARCHAR(24) NOT NULL,
    exception_reason VARCHAR(512),
    resolved_at DATETIME,
    resolved_by VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_reconciliation_row_import (import_id),
    INDEX idx_reconciliation_row_status (match_status),
    INDEX idx_reconciliation_row_order (matched_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS secondary_channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_code VARCHAR(64) NOT NULL UNIQUE,
    channel_name VARCHAR(128) NOT NULL,
    contact_name VARCHAR(64),
    contact_phone VARCHAR(32),
    settlement_info VARCHAR(512),
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS secondary_commission_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_name VARCHAR(128) NOT NULL,
    plan_code VARCHAR(64),
    plan_name VARCHAR(128),
    monthly_fee DECIMAL(12,2) NOT NULL,
    contract_months INT NOT NULL,
    main_multiplier DECIMAL(10,4) NOT NULL DEFAULT 0,
    extra_multiplier DECIMAL(10,4) NOT NULL DEFAULT 0,
    promotion_multiplier DECIMAL(10,4) NOT NULL DEFAULT 0,
    channel_multiplier DECIMAL(10,4) NOT NULL DEFAULT 0,
    default_channel_subsidy DECIMAL(12,2) NOT NULL DEFAULT 0,
    default_joincom_subsidy DECIMAL(12,2) NOT NULL DEFAULT 0,
    effective_from DATE,
    effective_to DATE,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_secondary_rule_plan (plan_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS secondary_commission_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    rule_snapshot JSON NOT NULL,
    promotion_applied TINYINT NOT NULL DEFAULT 0,
    joincom_total DECIMAL(12,2) NOT NULL,
    channel_gross DECIMAL(12,2) NOT NULL,
    channel_subsidy DECIMAL(12,2) NOT NULL DEFAULT 0,
    joincom_subsidy DECIMAL(12,2) NOT NULL DEFAULT 0,
    channel_payable DECIMAL(12,2) NOT NULL,
    joincom_retained DECIMAL(12,2) NOT NULL,
    t1_amount DECIMAL(12,2) NOT NULL,
    t3_amount DECIMAL(12,2) NOT NULL,
    t7_amount DECIMAL(12,2) NOT NULL,
    adjustment_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    final_amount DECIMAL(12,2) NOT NULL,
    adjustment_reason VARCHAR(512),
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    confirmed_by VARCHAR(64),
    confirmed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_secondary_commission_order (order_id),
    INDEX idx_secondary_commission_channel (channel_id),
    INDEX idx_secondary_commission_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_name VARCHAR(64) NOT NULL,
    operation_type VARCHAR(64) NOT NULL,
    object_type VARCHAR(64) NOT NULL,
    object_id VARCHAR(64),
    before_data JSON,
    after_data JSON,
    remark VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_operation_log_object (object_type, object_id),
    INDEX idx_operation_log_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_column_if_missing;
