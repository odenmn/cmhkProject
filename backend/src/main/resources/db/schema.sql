-- 仅用于全新安装的最终结构。真实数据库升级必须执行 db/migrations 下的版本化脚本。
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

CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    phone VARCHAR(32),
    email VARCHAR(128),
    role_code VARCHAR(32) NOT NULL DEFAULT 'ADMIN',
    scope_type VARCHAR(16) NOT NULL DEFAULT 'ALL'
        COMMENT '数据范围：ALL、CMHK、CHANNEL',
    scope_id BIGINT COMMENT 'CHANNEL范围对应channel.id，其他范围为空',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    last_login_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_admin_user_status (status),
    INDEX idx_admin_user_role_scope (role_code, scope_type, scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    customer_id BIGINT,
    plan_id BIGINT,
    plan_code VARCHAR(64),
    plan_name VARCHAR(128),
    plan_type VARCHAR(64),
    monthly_fee DECIMAL(10, 2),
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
    contact_phone VARCHAR(32),
    customer_identity TINYINT NOT NULL DEFAULT 0,
    has_offer TINYINT NOT NULL DEFAULT 0,
    has_pass_or_hkid TINYINT NOT NULL DEFAULT 0,
    expected_start_date DATE,
    id_type VARCHAR(32),
    id_no VARCHAR(64) COMMENT '历史兼容字段，应用不得采集、写入或返回',
    referrer_phone VARCHAR(32),
    preferred_contact_time VARCHAR(128),
    remark VARCHAR(512),
    status VARCHAR(32) NOT NULL,
    review_status VARCHAR(32) COMMENT 'UMALL审核状态',
    supplement_status VARCHAR(32) COMMENT 'UMALL补件状态',
    umall_order_no VARCHAR(64),
    service_number VARCHAR(32),
    activation_status VARCHAR(32),
    contract_status VARCHAR(32),
    order_source VARCHAR(32) NOT NULL DEFAULT 'H5',
    reconciliation_status VARCHAR(32) NOT NULL DEFAULT '待对账',
    source_record_id VARCHAR(64),
    source_channel_name VARCHAR(128),
    umall_status VARCHAR(32),
    status_updated_at DATETIME COMMENT '统一办理状态最近更新时间',
    onboard_date DATE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_mobile_plan_order_plan_id (plan_id),
    INDEX idx_mobile_plan_order_customer_id (customer_id),
    INDEX idx_mobile_plan_order_plan_code (plan_code),
    INDEX idx_mobile_plan_order_status (status),
    INDEX idx_mobile_plan_order_review_status (review_status),
    INDEX idx_mobile_plan_order_status_updated_at (status_updated_at),
    UNIQUE KEY uk_order_source_record (order_source, source_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_code VARCHAR(64) NOT NULL UNIQUE,
    channel_name VARCHAR(128) NOT NULL,
    channel_type VARCHAR(32) NOT NULL DEFAULT 'ORGANIZATION'
        COMMENT '渠道类型：RESOURCE、ORGANIZATION、ENTERPRISE、SALES_AGENT',
    parent_channel_id BIGINT,
    contact_name VARCHAR(64),
    contact_phone VARCHAR(32),
    cooperation_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
        COMMENT '合作状态：PENDING、ACTIVE、SUSPENDED、ENDED',
    settlement_info VARCHAR(512),
    owner_user_id BIGINT,
    elderly_mode TINYINT NOT NULL DEFAULT 0,
    wechat_service_url VARCHAR(512),
    wechat_qr_code_url VARCHAR(512),
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_channel_parent_id (parent_channel_id),
    INDEX idx_channel_owner_user_id (owner_user_id),
    INDEX idx_channel_cooperation_status (cooperation_status)
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
    phone VARCHAR(32) UNIQUE,
    phone_verified_at DATETIME,
    name VARCHAR(64),
    contact_method VARCHAR(128),
    customer_type VARCHAR(32) NOT NULL DEFAULT 'DIRECT',
    customer_category VARCHAR(32) COMMENT '业务客户类别，例如留学生、地产客户、研究生',
    channel_id BIGINT,
    owner_user_id BIGINT COMMENT 'JOINCOM内部负责人管理员ID',
    intended_plan VARCHAR(128),
    requirement_summary VARCHAR(512),
    current_status TINYINT NOT NULL DEFAULT 0
        COMMENT '客户状态码：0待处理，1跟进中，2待资料，3办理中，4待激活，5已激活，6已完成，9无效',
    source_system VARCHAR(32),
    source_customer_id VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_customer_source (source_system, source_customer_id),
    INDEX idx_customer_owner_user_id (owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customer_channel_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL UNIQUE,
    channel_id BIGINT NOT NULL,
    entry_id BIGINT,
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

-- JOINCOM 管理后台 MVP：客户、订单、ICCID、甲方对账和二级渠道结算。
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
    card_type VARCHAR(16) NOT NULL DEFAULT 'REAL',
    service_number VARCHAR(32),
    source_system VARCHAR(32),
    source_record_id VARCHAR(64),
    replaced_by_iccid_id BIGINT,
    replaced_at DATETIME,
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
    umall_status VARCHAR(64),
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

CREATE TABLE IF NOT EXISTS channel_legacy_mapping (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    legacy_table VARCHAR(64) NOT NULL,
    legacy_id BIGINT NOT NULL,
    legacy_channel_code VARCHAR(64) NOT NULL,
    channel_id BIGINT,
    migration_status VARCHAR(16) NOT NULL,
    conflict_reason VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_channel_legacy_source (legacy_table, legacy_id),
    INDEX idx_channel_legacy_channel_id (channel_id),
    INDEX idx_channel_legacy_status (migration_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS channel_migration_exception (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    legacy_table VARCHAR(64) NOT NULL,
    legacy_id BIGINT NOT NULL,
    channel_code VARCHAR(64),
    exception_type VARCHAR(32) NOT NULL,
    exception_detail VARCHAR(512) NOT NULL,
    resolution_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    resolved_channel_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,
    UNIQUE KEY uk_channel_migration_exception (legacy_table, legacy_id, exception_type),
    INDEX idx_channel_migration_exception_status (resolution_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customer_backup_import (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    file_hash CHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    total_count INT NOT NULL DEFAULT 0,
    customer_count INT NOT NULL DEFAULT 0,
    order_count INT NOT NULL DEFAULT 0,
    iccid_count INT NOT NULL DEFAULT 0,
    exception_count INT NOT NULL DEFAULT 0,
    operator_name VARCHAR(64) NOT NULL,
    confirmed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_customer_backup_file_hash (file_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customer_backup_import_row (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_id BIGINT NOT NULL,
    source_row_number INT NOT NULL,
    source_id VARCHAR(64),
    customer_id BIGINT,
    order_id BIGINT,
    iccid_id BIGINT,
    result_status VARCHAR(24) NOT NULL,
    exception_code VARCHAR(64),
    exception_reason VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_customer_backup_row_import (import_id),
    INDEX idx_customer_backup_row_source (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customer_follow_up (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    follow_up_type VARCHAR(32) NOT NULL COMMENT '跟进类型',
    content VARCHAR(1000) NOT NULL COMMENT '跟进内容，不保存正式身份资料',
    next_follow_up_at DATETIME,
    operator_user_id BIGINT,
    operator_name VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_customer_follow_up_customer (customer_id),
    INDEX idx_customer_follow_up_next_time (next_follow_up_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    status_type VARCHAR(32) NOT NULL COMMENT 'JOINCOM、UMALL、UMALL_REVIEW、UMALL_SUPPLEMENT、ACTIVATION、CONTRACT',
    before_status VARCHAR(32),
    after_status VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL COMMENT 'ADMIN、H5、RECONCILIATION、MIGRATION、SYSTEM',
    operator_user_id BIGINT,
    operator_name VARCHAR(64),
    remark VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_status_history_order (order_id),
    INDEX idx_order_status_history_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS channel_product_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    promotable TINYINT NOT NULL DEFAULT 1,
    effective_from DATE,
    effective_to DATE,
    cashback_rule_ref VARCHAR(128),
    commission_rule_ref VARCHAR(128),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_channel_product_policy (channel_id, plan_id),
    INDEX idx_channel_product_policy_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
