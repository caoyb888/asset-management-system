-- ============================================================
-- 资产管理系统 - 基础数据管理模块初始化脚本
-- 数据库: asset_db
-- 字符集: utf8mb4 / utf8mb4_unicode_ci
-- 引擎: InnoDB
-- MySQL: 8.0+
-- 来源: docs/基础数据管理模块数据库设计.md（修订版 2026-02-16）
-- 修订内容:
--   · DECIMAL(14,2)  支持百万平米/大额金额
--   · SM4 国密加密  敏感字段(id_card)
--   · biz_shop_relation  M:N 商铺拆合溯源
--   · 复合唯一索引(+is_deleted)  支持逻辑删除后重建
--   · 全表补齐五件套审计字段
-- ============================================================

CREATE DATABASE IF NOT EXISTS asset_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE asset_db;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 系统基础表
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_company (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    company_code VARCHAR(50)  NOT NULL UNIQUE  COMMENT '公司编码',
    company_name VARCHAR(200) NOT NULL          COMMENT '公司名称',
    status      TINYINT DEFAULT 1               COMMENT '状态: 0停用 1启用',
    created_by  BIGINT UNSIGNED                 COMMENT '创建人ID',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by  BIGINT UNSIGNED                 COMMENT '更新人ID',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_company_code (company_code),
    INDEX idx_status       (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公司表';


CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username    VARCHAR(50)  NOT NULL UNIQUE  COMMENT '用户名',
    password    VARCHAR(64)  NOT NULL         COMMENT 'SM3密码哈希',
    real_name   VARCHAR(50)                  COMMENT '真实姓名',
    status      TINYINT DEFAULT 1             COMMENT '状态: 0停用 1启用',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 默认管理员 admin / admin123（SM3哈希）
INSERT IGNORE INTO sys_user (username, password, real_name, status)
VALUES ('admin', '667c756cf9334e328a56e44e906245c8e214c655a160f18fdb84d79c209c49cf', '系统管理员', 1);

-- 默认公司数据
INSERT IGNORE INTO sys_company (company_code, company_name, status) VALUES
  ('CC001', '产城（总部）投资管理有限公司', 1),
  ('CC002', '产城（北京）资产管理有限公司', 1),
  ('CC003', '产城（上海）资产管理有限公司', 1);


-- ============================================================
-- 2. 项目管理相关表
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_project (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_code     VARCHAR(50)  NOT NULL               COMMENT '项目编号',
    project_name     VARCHAR(200) NOT NULL               COMMENT '项目名称',
    company_id       BIGINT UNSIGNED NOT NULL            COMMENT '所属公司ID',
    province         VARCHAR(50)                         COMMENT '所在省份',
    city             VARCHAR(50)                         COMMENT '所在城市',
    address          VARCHAR(500)                        COMMENT '项目地址',
    property_type    TINYINT                             COMMENT '产权性质: 1国有 2集体 3私有 4其他',
    business_type    TINYINT                             COMMENT '经营类型: 1自持 2租赁 3合作',
    building_area    DECIMAL(14,2) DEFAULT 0             COMMENT '建筑面积(㎡)',
    operating_area   DECIMAL(14,2) DEFAULT 0             COMMENT '经营面积(㎡)',
    operation_status TINYINT DEFAULT 0                   COMMENT '运营状态: 0筹备 1开业 2停业',
    opening_date     DATE                                COMMENT '开业时间',
    manager_id       BIGINT UNSIGNED                     COMMENT '负责人ID',
    image_urls       JSON                                COMMENT '项目图片URL数组(JSON)',
    is_deleted       TINYINT DEFAULT 0                   COMMENT '逻辑删除: 0正常 1删除',
    created_by       BIGINT UNSIGNED                     COMMENT '创建人ID',
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_by       BIGINT UNSIGNED                     COMMENT '更新人ID',
    updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_project_code_deleted (project_code, is_deleted),
    INDEX idx_company_id       (company_id),
    INDEX idx_manager_id       (manager_id),
    INDEX idx_operation_status (operation_status),
    INDEX idx_project_name     (project_name),
    INDEX idx_is_deleted       (is_deleted),
    INDEX idx_province_city    (province, city, is_deleted),
    INDEX idx_project_manager  (manager_id, is_deleted, operation_status),

    FOREIGN KEY (company_id) REFERENCES sys_company(id) ON DELETE RESTRICT,
    FOREIGN KEY (manager_id) REFERENCES sys_user(id)    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';


CREATE TABLE IF NOT EXISTS biz_project_contract (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id          BIGINT UNSIGNED NOT NULL   COMMENT '项目ID',
    party_a_name        VARCHAR(200)               COMMENT '合同甲方抬头',
    party_a_abbr        VARCHAR(100)               COMMENT '合同甲方缩写',
    party_a_address     VARCHAR(500)               COMMENT '甲方地址',
    party_a_phone       VARCHAR(30)                COMMENT '甲方电话',
    business_license    VARCHAR(200)               COMMENT '营业执照号',
    legal_representative VARCHAR(50)               COMMENT '法人代表',
    email               VARCHAR(100)               COMMENT '邮箱',
    is_deleted          TINYINT DEFAULT 0          COMMENT '逻辑删除',
    created_by          BIGINT UNSIGNED            COMMENT '创建人ID',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by          BIGINT UNSIGNED            COMMENT '更新人ID',
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_project_id (project_id),
    INDEX idx_is_deleted (is_deleted),
    FOREIGN KEY (project_id) REFERENCES biz_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目合同甲方信息表';


CREATE TABLE IF NOT EXISTS biz_project_finance_contact (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id   BIGINT UNSIGNED NOT NULL  COMMENT '项目ID',
    contact_name VARCHAR(50)  NOT NULL     COMMENT '联系人姓名',
    phone        VARCHAR(30)               COMMENT '电话',
    email        VARCHAR(100)              COMMENT '邮箱',
    credit_code  VARCHAR(50)               COMMENT '社会信用代码',
    seal_type    VARCHAR(50)               COMMENT '用章类型',
    seal_desc    VARCHAR(200)              COMMENT '用章说明',
    is_deleted   TINYINT DEFAULT 0         COMMENT '逻辑删除',
    created_by   BIGINT UNSIGNED           COMMENT '创建人ID',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by   BIGINT UNSIGNED           COMMENT '更新人ID',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_project_id (project_id),
    INDEX idx_is_deleted (is_deleted),
    FOREIGN KEY (project_id) REFERENCES biz_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务联系人表';


CREATE TABLE IF NOT EXISTS biz_project_bank (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id   BIGINT UNSIGNED NOT NULL  COMMENT '项目ID',
    bank_name    VARCHAR(200)              COMMENT '银行名称',
    bank_account VARCHAR(50)               COMMENT '银行账号',
    account_name VARCHAR(100)              COMMENT '户名',
    is_default   TINYINT DEFAULT 0         COMMENT '是否默认: 0否 1是',
    is_deleted   TINYINT DEFAULT 0         COMMENT '逻辑删除',
    created_by   BIGINT UNSIGNED           COMMENT '创建人ID',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by   BIGINT UNSIGNED           COMMENT '更新人ID',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_project_id (project_id),
    INDEX idx_is_deleted (is_deleted),
    FOREIGN KEY (project_id) REFERENCES biz_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='银行账号表';


-- ============================================================
-- 3. 楼栋楼层表
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_building (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id     BIGINT UNSIGNED NOT NULL   COMMENT '所属项目ID',
    building_code  VARCHAR(50)                COMMENT '楼栋编码',
    building_name  VARCHAR(200) NOT NULL      COMMENT '楼栋名称',
    status         TINYINT DEFAULT 1          COMMENT '状态: 0停用 1启用',
    building_area  DECIMAL(14,2) DEFAULT 0    COMMENT '建筑面积(㎡)',
    operating_area DECIMAL(14,2) DEFAULT 0    COMMENT '营业面积(㎡)',
    above_floors   INT DEFAULT 0              COMMENT '地上楼层数',
    below_floors   INT DEFAULT 0              COMMENT '地下楼层数',
    image_url      VARCHAR(500)               COMMENT '楼栋平面图URL',
    is_deleted     TINYINT DEFAULT 0          COMMENT '逻辑删除',
    created_by     BIGINT UNSIGNED            COMMENT '创建人ID',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by     BIGINT UNSIGNED            COMMENT '更新人ID',
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_building_code_project (project_id, building_code, is_deleted),
    INDEX idx_building_name (building_name),
    INDEX idx_status        (status),
    INDEX idx_is_deleted    (is_deleted),

    FOREIGN KEY (project_id) REFERENCES biz_project(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='楼栋表';


CREATE TABLE IF NOT EXISTS biz_floor (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id     BIGINT UNSIGNED NOT NULL   COMMENT '所属项目ID',
    building_id    BIGINT UNSIGNED NOT NULL   COMMENT '所属楼栋ID',
    floor_code     VARCHAR(50)                COMMENT '楼层编码',
    floor_name     VARCHAR(100) NOT NULL      COMMENT '楼层名称',
    status         TINYINT DEFAULT 1          COMMENT '状态: 0停用 1启用',
    building_area  DECIMAL(14,2) DEFAULT 0    COMMENT '建筑面积(㎡)',
    operating_area DECIMAL(14,2) DEFAULT 0    COMMENT '营业面积(㎡)',
    remark         VARCHAR(500)               COMMENT '备注',
    image_url      VARCHAR(500)               COMMENT '楼层平面图URL',
    is_deleted     TINYINT DEFAULT 0          COMMENT '逻辑删除',
    created_by     BIGINT UNSIGNED            COMMENT '创建人ID',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by     BIGINT UNSIGNED            COMMENT '更新人ID',
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_floor_code_building (building_id, floor_code, is_deleted),
    INDEX idx_project_id  (project_id),
    INDEX idx_building_id (building_id),
    INDEX idx_status      (status),
    INDEX idx_is_deleted  (is_deleted),

    FOREIGN KEY (project_id)  REFERENCES biz_project(id)  ON DELETE RESTRICT,
    FOREIGN KEY (building_id) REFERENCES biz_building(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='楼层表';


-- ============================================================
-- 4. 商铺表
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_shop (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id          BIGINT UNSIGNED NOT NULL   COMMENT '所属项目ID',
    building_id         BIGINT UNSIGNED NOT NULL   COMMENT '所属楼栋ID',
    floor_id            BIGINT UNSIGNED NOT NULL   COMMENT '所在楼层ID',
    shop_code           VARCHAR(50)  NOT NULL      COMMENT '铺位号',
    shop_type           TINYINT                    COMMENT '商铺类型: 1临街 2内铺 3专柜',
    rent_area           DECIMAL(14,2) DEFAULT 0    COMMENT '计租面积(㎡)',
    measured_area       DECIMAL(14,2) DEFAULT 0    COMMENT '实测面积(㎡)',
    building_area       DECIMAL(14,2) DEFAULT 0    COMMENT '建筑面积(㎡)',
    operating_area      DECIMAL(14,2) DEFAULT 0    COMMENT '经营面积(㎡)',
    shop_status         TINYINT DEFAULT 0          COMMENT '商铺状态: 0空置 1在租 2自用 3预留',
    count_leasing_rate  TINYINT DEFAULT 1          COMMENT '计入招商率: 0否 1是',
    count_rental_rate   TINYINT DEFAULT 1          COMMENT '计入出租率: 0否 1是',
    count_opening_rate  TINYINT DEFAULT 1          COMMENT '计入开业率: 0否 1是',
    signed_format       VARCHAR(100)               COMMENT '签约业态',
    planned_format      VARCHAR(100)               COMMENT '规划业态',
    owner_name          VARCHAR(100)               COMMENT '业主名称',
    owner_contact       VARCHAR(50)                COMMENT '业主联系人',
    owner_phone         VARCHAR(30)                COMMENT '业主电话',
    is_deleted          TINYINT DEFAULT 0          COMMENT '逻辑删除: 0正常 1删除',
    created_by          BIGINT UNSIGNED            COMMENT '创建人ID',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by          BIGINT UNSIGNED            COMMENT '更新人ID',
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 复合唯一索引：项目内铺位号唯一，支持逻辑删除后重建
    UNIQUE KEY uk_shop_code_project (project_id, shop_code, is_deleted),
    INDEX idx_building_id          (building_id),
    INDEX idx_floor_id             (floor_id),
    INDEX idx_shop_status          (shop_status),
    INDEX idx_project_status       (project_id, shop_status, is_deleted),
    INDEX idx_is_deleted           (is_deleted),

    FOREIGN KEY (project_id)  REFERENCES biz_project(id)  ON DELETE RESTRICT,
    FOREIGN KEY (building_id) REFERENCES biz_building(id) ON DELETE RESTRICT,
    FOREIGN KEY (floor_id)    REFERENCES biz_floor(id)    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商铺表';


-- ============================================================
-- 5. 商铺拆合关系表（M:N 溯源）
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_shop_relation (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    source_shop_id BIGINT UNSIGNED NOT NULL   COMMENT '源商铺ID（拆分前/合并前）',
    target_shop_id BIGINT UNSIGNED NOT NULL   COMMENT '目标商铺ID（拆分后/合并后）',
    relation_type  TINYINT NOT NULL           COMMENT '关系类型: 1拆分 2合并',
    area_before    DECIMAL(14,2) DEFAULT 0    COMMENT '变更前面积(㎡)',
    area_after     DECIMAL(14,2) DEFAULT 0    COMMENT '变更后面积(㎡)',
    remark         VARCHAR(500)               COMMENT '操作备注',
    is_deleted     TINYINT DEFAULT 0          COMMENT '逻辑删除',
    created_by     BIGINT UNSIGNED            COMMENT '操作人ID',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    INDEX idx_source_shop         (source_shop_id),
    INDEX idx_target_shop         (target_shop_id),
    INDEX idx_relation_type       (relation_type),
    INDEX idx_created_at          (created_at),
    INDEX idx_shop_relation_query (source_shop_id, target_shop_id, relation_type),
    INDEX idx_shop_relation_time  (created_at, relation_type),

    FOREIGN KEY (source_shop_id) REFERENCES biz_shop(id) ON DELETE RESTRICT,
    FOREIGN KEY (target_shop_id) REFERENCES biz_shop(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商铺拆合关系表';


-- ============================================================
-- 6. 品牌管理表
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_brand (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    brand_code         VARCHAR(50)             COMMENT '品牌编码',
    brand_name_cn      VARCHAR(200) NOT NULL   COMMENT '品牌名(中)',
    brand_name_en      VARCHAR(200)            COMMENT '品牌名(英)',
    format_type        VARCHAR(100)            COMMENT '所属业态',
    brand_level        TINYINT                 COMMENT '品牌等级: 1高端 2中端 3大众',
    cooperation_type   TINYINT                 COMMENT '合作关系: 1直营 2加盟 3代理',
    business_nature    TINYINT                 COMMENT '经营性质: 1餐饮 2零售 3娱乐 4服务',
    chain_type         TINYINT                 COMMENT '连锁类型: 1连锁 2单店',
    project_stage      VARCHAR(50)             COMMENT '项目阶段',
    group_name         VARCHAR(200)            COMMENT '集团名称',
    hq_address         VARCHAR(500)            COMMENT '总部地址',
    main_cities        VARCHAR(500)            COMMENT '主要分布城市',
    website            VARCHAR(300)            COMMENT '网址',
    phone              VARCHAR(30)             COMMENT '联系电话',
    brand_type         TINYINT                 COMMENT '品牌类型: 1MALL 2商街',
    avg_rent           DECIMAL(14,2)           COMMENT '平均租金(元/㎡·月)',
    min_customer_price DECIMAL(14,2)           COMMENT '最低客单价(元)',
    brand_intro        TEXT                    COMMENT '品牌简介',
    is_deleted         TINYINT DEFAULT 0       COMMENT '逻辑删除',
    created_by         BIGINT UNSIGNED         COMMENT '创建人ID',
    created_at         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by         BIGINT UNSIGNED         COMMENT '更新人ID',
    updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_brand_code_deleted (brand_code, is_deleted),
    INDEX idx_brand_name_cn (brand_name_cn),
    INDEX idx_format_type   (format_type),
    INDEX idx_brand_level   (brand_level),
    INDEX idx_is_deleted    (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='品牌表';


CREATE TABLE IF NOT EXISTS biz_brand_contact (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    brand_id     BIGINT UNSIGNED NOT NULL  COMMENT '品牌ID',
    contact_name VARCHAR(50)               COMMENT '联系人姓名',
    phone        VARCHAR(30)               COMMENT '电话',
    email        VARCHAR(100)              COMMENT '邮箱',
    position     VARCHAR(50)               COMMENT '职位',
    is_primary   TINYINT DEFAULT 0         COMMENT '是否主要联系人: 0否 1是',
    is_deleted   TINYINT DEFAULT 0         COMMENT '逻辑删除',
    created_by   BIGINT UNSIGNED           COMMENT '创建人ID',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by   BIGINT UNSIGNED           COMMENT '更新人ID',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_brand_id   (brand_id),
    INDEX idx_is_primary (is_primary),
    INDEX idx_is_deleted (is_deleted),
    FOREIGN KEY (brand_id) REFERENCES biz_brand(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='品牌联系人表';


-- ============================================================
-- 7. 商家管理表
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_merchant (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id      BIGINT UNSIGNED NOT NULL   COMMENT '所属项目ID',
    merchant_code   VARCHAR(50)                COMMENT '商家编号',
    merchant_name   VARCHAR(200) NOT NULL      COMMENT '商家名称',
    merchant_attr   TINYINT                    COMMENT '商家属性: 1个体户 2企业',
    merchant_nature TINYINT                    COMMENT '商家性质: 1民营 2国营 3外资 4合资',
    format_type     VARCHAR(100)               COMMENT '经营业态',
    natural_person  VARCHAR(50)                COMMENT '自然人姓名',
    id_card         VARCHAR(200)               COMMENT '身份证号(SM4加密存储)',
    address         VARCHAR(500)               COMMENT '地址',
    phone           VARCHAR(30)                COMMENT '手机',
    merchant_level  TINYINT DEFAULT 3          COMMENT '商家评级: 1优秀 2良好 3一般 4差',
    audit_status    TINYINT DEFAULT 0          COMMENT '审核状态: 0待审核 1通过 2驳回',
    is_deleted      TINYINT DEFAULT 0          COMMENT '逻辑删除',
    created_by      BIGINT UNSIGNED            COMMENT '创建人ID',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by      BIGINT UNSIGNED            COMMENT '更新人ID',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_merchant_code_project (project_id, merchant_code, is_deleted),
    INDEX idx_project_id         (project_id),
    INDEX idx_merchant_name      (merchant_name),
    INDEX idx_audit_status       (audit_status),
    INDEX idx_merchant_level     (merchant_level),
    INDEX idx_is_deleted         (is_deleted),
    INDEX idx_merchant_audit_query (project_id, audit_status, created_at),

    FOREIGN KEY (project_id) REFERENCES biz_project(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家表';


CREATE TABLE IF NOT EXISTS biz_merchant_contact (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    merchant_id  BIGINT UNSIGNED NOT NULL  COMMENT '商家ID',
    contact_name VARCHAR(50)               COMMENT '联系人姓名',
    phone        VARCHAR(30)               COMMENT '电话',
    email        VARCHAR(100)              COMMENT '邮箱',
    position     VARCHAR(50)               COMMENT '职位',
    is_primary   TINYINT DEFAULT 0         COMMENT '是否主要联系人',
    is_deleted   TINYINT DEFAULT 0         COMMENT '逻辑删除',
    created_by   BIGINT UNSIGNED           COMMENT '创建人ID',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by   BIGINT UNSIGNED           COMMENT '更新人ID',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_merchant_id (merchant_id),
    INDEX idx_is_deleted  (is_deleted),
    FOREIGN KEY (merchant_id) REFERENCES biz_merchant(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家联系人表';


CREATE TABLE IF NOT EXISTS biz_merchant_invoice (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    merchant_id  BIGINT UNSIGNED NOT NULL  COMMENT '商家ID',
    invoice_title VARCHAR(200)             COMMENT '发票抬头',
    tax_number   VARCHAR(50)               COMMENT '税号',
    bank_name    VARCHAR(200)              COMMENT '开户银行',
    bank_account VARCHAR(50)               COMMENT '银行账号',
    address      VARCHAR(500)              COMMENT '注册地址',
    phone        VARCHAR(30)               COMMENT '注册电话',
    is_default   TINYINT DEFAULT 0         COMMENT '是否默认',
    is_deleted   TINYINT DEFAULT 0         COMMENT '逻辑删除',
    created_by   BIGINT UNSIGNED           COMMENT '创建人ID',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by   BIGINT UNSIGNED           COMMENT '更新人ID',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_merchant_id (merchant_id),
    INDEX idx_is_default  (is_default),
    INDEX idx_is_deleted  (is_deleted),
    FOREIGN KEY (merchant_id) REFERENCES biz_merchant(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家开票信息表';


CREATE TABLE IF NOT EXISTS biz_merchant_credit (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    merchant_id    BIGINT UNSIGNED NOT NULL   COMMENT '商家ID',
    record_type    TINYINT                    COMMENT '记录类型: 1好评 2差评 3违约 4其他',
    content        TEXT                       COMMENT '记录内容',
    record_date    DATE                       COMMENT '记录日期',
    operator_id    BIGINT UNSIGNED            COMMENT '操作人ID',
    attachment_url VARCHAR(500)               COMMENT '附件URL',
    is_deleted     TINYINT DEFAULT 0          COMMENT '逻辑删除',
    created_by     BIGINT UNSIGNED            COMMENT '创建人ID',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by     BIGINT UNSIGNED            COMMENT '更新人ID',
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_merchant_id (merchant_id),
    INDEX idx_record_type (record_type),
    INDEX idx_is_deleted  (is_deleted),
    FOREIGN KEY (merchant_id) REFERENCES biz_merchant(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家诚信记录表';


CREATE TABLE IF NOT EXISTS biz_merchant_attachment (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    merchant_id BIGINT UNSIGNED NOT NULL  COMMENT '商家ID',
    file_name   VARCHAR(200)              COMMENT '文件名称',
    file_url    VARCHAR(500)              COMMENT '文件URL',
    file_type   VARCHAR(50)               COMMENT '文件类型',
    file_size   BIGINT                    COMMENT '文件大小(字节)',
    upload_by   BIGINT UNSIGNED           COMMENT '上传人ID',
    is_deleted  TINYINT DEFAULT 0         COMMENT '逻辑删除',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_merchant_id (merchant_id),
    INDEX idx_is_deleted  (is_deleted),
    FOREIGN KEY (merchant_id) REFERENCES biz_merchant(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家附件表';


-- ============================================================
-- 8. 内容管理表
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_notice (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title          VARCHAR(300) NOT NULL      COMMENT '标题',
    content        LONGTEXT                   COMMENT '内容(富文本HTML)',
    notice_type    TINYINT                    COMMENT '类型: 1通知 2公告 3政策',
    status         TINYINT DEFAULT 0          COMMENT '状态: 0草稿 1已发布 2下架',
    scheduled_time DATETIME                   COMMENT '定时发送时间',
    publish_time   DATETIME                   COMMENT '实际发布时间',
    created_by     BIGINT UNSIGNED            COMMENT '创建人ID',
    is_deleted     TINYINT DEFAULT 0          COMMENT '逻辑删除',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by     BIGINT UNSIGNED            COMMENT '更新人ID',
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_notice_type    (notice_type),
    INDEX idx_status         (status),
    INDEX idx_scheduled_time (scheduled_time),
    INDEX idx_created_by     (created_by),
    INDEX idx_is_deleted     (is_deleted)
    -- 注: 超过5000条后建议改用 Elasticsearch 全文搜索
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知公告表';


CREATE TABLE IF NOT EXISTS biz_notice_read (
    id        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    notice_id BIGINT UNSIGNED NOT NULL   COMMENT '公告ID',
    user_id   BIGINT UNSIGNED NOT NULL   COMMENT '用户ID',
    read_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    is_deleted TINYINT DEFAULT 0         COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_notice_user (notice_id, user_id),
    INDEX idx_user_id    (user_id),
    INDEX idx_is_deleted (is_deleted),
    FOREIGN KEY (notice_id) REFERENCES biz_notice(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告阅读记录表';


CREATE TABLE IF NOT EXISTS biz_news (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title        VARCHAR(300) NOT NULL   COMMENT '标题',
    content      LONGTEXT                COMMENT '内容(富文本HTML)',
    category     TINYINT                 COMMENT '分类: 1新闻 2政策 3招商 4服务指南',
    status       TINYINT DEFAULT 0       COMMENT '状态: 0草稿 1上架 2下架',
    publish_time DATETIME                COMMENT '发布时间',
    created_by   BIGINT UNSIGNED         COMMENT '创建人ID',
    is_deleted   TINYINT DEFAULT 0       COMMENT '逻辑删除',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by   BIGINT UNSIGNED         COMMENT '更新人ID',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_category     (category),
    INDEX idx_status       (status),
    INDEX idx_publish_time (publish_time),
    INDEX idx_created_by   (created_by),
    INDEX idx_is_deleted   (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新闻资讯表';


-- ============================================================
-- 9. 预留：虚拟列索引（按需手动开启）
-- ============================================================
-- 项目图片数量虚拟列索引（高频按图片数量查询时启用）
-- ALTER TABLE biz_project
--   ADD COLUMN image_count INT GENERATED ALWAYS AS (JSON_LENGTH(image_urls)) STORED,
--   ADD INDEX idx_image_count (image_count);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 脚本执行完毕
-- 共创建数据库表: 17 张
--   系统表: sys_company, sys_user
--   项目表: biz_project, biz_project_contract,
--           biz_project_finance_contact, biz_project_bank
--   楼栋楼层: biz_building, biz_floor
--   商铺: biz_shop, biz_shop_relation
--   品牌: biz_brand, biz_brand_contact
--   商家: biz_merchant, biz_merchant_contact, biz_merchant_invoice,
--         biz_merchant_credit, biz_merchant_attachment
--   内容: biz_notice, biz_notice_read, biz_news
-- ============================================================
