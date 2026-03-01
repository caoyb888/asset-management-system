-- ============================================================
-- 资产管理系统 - 系统管理模块初始化脚本
-- 数据库: asset_db
-- 说明: 在 init.sql 执行之后运行
--   · ALTER sys_user 补全扩展字段
--   · 新建 sys_dept / sys_post / sys_user_post
--   · 新建 sys_role / sys_user_role
--   · 新建 sys_menu / sys_role_menu
--   · 新建 sys_dict_type / sys_dict_data
--   · 新建 sys_oper_log
-- ============================================================

USE asset_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 扩展 sys_user 表（补全系统管理所需字段）
-- ============================================================
ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS dept_id      BIGINT UNSIGNED              COMMENT '所属部门ID',
    ADD COLUMN IF NOT EXISTS phone        VARCHAR(20)  DEFAULT ''       COMMENT '手机号(SM4加密)',
    ADD COLUMN IF NOT EXISTS email        VARCHAR(100) DEFAULT ''       COMMENT '邮箱',
    ADD COLUMN IF NOT EXISTS avatar       VARCHAR(500) DEFAULT ''       COMMENT '头像URL',
    ADD COLUMN IF NOT EXISTS login_ip     VARCHAR(128) DEFAULT ''       COMMENT '最后登录IP',
    ADD COLUMN IF NOT EXISTS login_time   DATETIME                      COMMENT '最后登录时间',
    ADD COLUMN IF NOT EXISTS is_deleted   TINYINT      DEFAULT 0        COMMENT '逻辑删除: 0正常 1删除',
    ADD COLUMN IF NOT EXISTS created_by   BIGINT UNSIGNED               COMMENT '创建人ID',
    ADD COLUMN IF NOT EXISTS updated_by   BIGINT UNSIGNED               COMMENT '更新人ID';

-- ============================================================
-- 2. 部门/机构表（支持无限层级树形结构）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_dept (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID',
    parent_id     BIGINT UNSIGNED     DEFAULT 0       COMMENT '父部门ID（0=顶级）',
    ancestors     VARCHAR(500)        DEFAULT ''       COMMENT '祖级列表（逗号分隔ID）',
    dept_name     VARCHAR(100)        NOT NULL         COMMENT '部门名称',
    dept_code     VARCHAR(50)                          COMMENT '部门编码',
    sort_order    INT                 DEFAULT 0        COMMENT '排序',
    leader        VARCHAR(50)         DEFAULT ''       COMMENT '负责人',
    phone         VARCHAR(20)         DEFAULT ''       COMMENT '联系电话',
    email         VARCHAR(100)        DEFAULT ''       COMMENT '邮箱',
    status        TINYINT             DEFAULT 1        COMMENT '状态: 0停用 1正常',
    is_deleted    TINYINT             DEFAULT 0        COMMENT '逻辑删除: 0正常 1删除',
    created_by    BIGINT UNSIGNED                      COMMENT '创建人ID',
    created_at    DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by    BIGINT UNSIGNED                      COMMENT '更新人ID',
    updated_at    DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id  (parent_id),
    INDEX idx_status     (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门/机构表';

-- ============================================================
-- 3. 岗位表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_post (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '岗位ID',
    post_code     VARCHAR(50)         NOT NULL         COMMENT '岗位编码',
    post_name     VARCHAR(100)        NOT NULL         COMMENT '岗位名称',
    sort_order    INT                 DEFAULT 0        COMMENT '排序',
    status        TINYINT             DEFAULT 1        COMMENT '状态: 0停用 1正常',
    remark        VARCHAR(500)        DEFAULT ''       COMMENT '备注',
    is_deleted    TINYINT             DEFAULT 0        COMMENT '逻辑删除: 0正常 1删除',
    created_by    BIGINT UNSIGNED                      COMMENT '创建人ID',
    created_at    DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by    BIGINT UNSIGNED                      COMMENT '更新人ID',
    updated_at    DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_post_code (post_code),
    INDEX idx_status     (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位表';

-- ============================================================
-- 4. 用户岗位关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user_post (
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    post_id    BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
    PRIMARY KEY (user_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户岗位关联表';

-- ============================================================
-- 5. 角色表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    role_name     VARCHAR(50)         NOT NULL         COMMENT '角色名称',
    role_code     VARCHAR(50)         NOT NULL         COMMENT '角色编码',
    data_scope    TINYINT             DEFAULT 1        COMMENT '数据权限: 1全部 2自定义 3本部门 4本部门及以下 5本人',
    sort_order    INT                 DEFAULT 0        COMMENT '排序',
    status        TINYINT             DEFAULT 1        COMMENT '状态: 0停用 1正常',
    remark        VARCHAR(500)        DEFAULT ''       COMMENT '备注',
    is_deleted    TINYINT             DEFAULT 0        COMMENT '逻辑删除: 0正常 1删除',
    created_by    BIGINT UNSIGNED                      COMMENT '创建人ID',
    created_at    DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by    BIGINT UNSIGNED                      COMMENT '更新人ID',
    updated_at    DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_role_code (role_code),
    INDEX idx_status     (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ============================================================
-- 6. 用户角色关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    role_id    BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ============================================================
-- 7. 菜单/权限表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_menu (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '菜单ID',
    parent_id     BIGINT UNSIGNED     DEFAULT 0        COMMENT '父菜单ID（0=顶级）',
    menu_name     VARCHAR(100)        NOT NULL          COMMENT '菜单名称',
    menu_type     CHAR(1)             NOT NULL          COMMENT '类型: M目录 C菜单 F按钮/权限',
    path          VARCHAR(200)        DEFAULT ''        COMMENT '路由地址',
    component     VARCHAR(255)        DEFAULT ''        COMMENT '组件路径',
    perms         VARCHAR(200)        DEFAULT ''        COMMENT '权限标识(如: sys:user:list)',
    icon          VARCHAR(100)        DEFAULT ''        COMMENT '图标',
    sort_order    INT                 DEFAULT 0         COMMENT '排序',
    visible       TINYINT             DEFAULT 1         COMMENT '是否显示: 0隐藏 1显示',
    status        TINYINT             DEFAULT 1         COMMENT '状态: 0停用 1正常',
    remark        VARCHAR(500)        DEFAULT ''        COMMENT '备注',
    created_at    DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_menu_type (menu_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

-- ============================================================
-- 8. 角色菜单关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id    BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    menu_id    BIGINT UNSIGNED NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- ============================================================
-- 9. 字典类型表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '字典类型ID',
    dict_name     VARCHAR(100)        NOT NULL         COMMENT '字典名称',
    dict_type     VARCHAR(100)        NOT NULL         COMMENT '字典类型标识',
    status        TINYINT             DEFAULT 1        COMMENT '状态: 0停用 1正常',
    remark        VARCHAR(500)        DEFAULT ''       COMMENT '备注',
    is_deleted    TINYINT             DEFAULT 0        COMMENT '逻辑删除: 0正常 1删除',
    created_by    BIGINT UNSIGNED                      COMMENT '创建人ID',
    created_at    DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by    BIGINT UNSIGNED                      COMMENT '更新人ID',
    updated_at    DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_dict_type (dict_type),
    INDEX idx_status     (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';

-- ============================================================
-- 10. 字典数据表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_dict_data (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '字典数据ID',
    dict_type     VARCHAR(100)        NOT NULL         COMMENT '字典类型标识',
    dict_label    VARCHAR(100)        NOT NULL         COMMENT '字典标签',
    dict_value    VARCHAR(200)        NOT NULL         COMMENT '字典值',
    css_class     VARCHAR(100)        DEFAULT ''       COMMENT '样式属性（el-tag type）',
    sort_order    INT                 DEFAULT 0        COMMENT '排序',
    status        TINYINT             DEFAULT 1        COMMENT '状态: 0停用 1正常',
    remark        VARCHAR(500)        DEFAULT ''       COMMENT '备注',
    is_deleted    TINYINT             DEFAULT 0        COMMENT '逻辑删除: 0正常 1删除',
    created_by    BIGINT UNSIGNED                      COMMENT '创建人ID',
    created_at    DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by    BIGINT UNSIGNED                      COMMENT '更新人ID',
    updated_at    DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_dict_type  (dict_type),
    INDEX idx_status     (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';

-- ============================================================
-- 11. 操作日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_oper_log (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    module          VARCHAR(50)         DEFAULT ''       COMMENT '模块名称',
    biz_type        VARCHAR(50)         DEFAULT ''       COMMENT '业务类型',
    method          VARCHAR(200)        DEFAULT ''       COMMENT '请求方法（类.方法）',
    request_method  VARCHAR(10)         DEFAULT ''       COMMENT 'HTTP方法',
    request_url     VARCHAR(500)        DEFAULT ''       COMMENT '请求URL',
    request_param   TEXT                                 COMMENT '请求参数(JSON)',
    response_result TEXT                                 COMMENT '响应结果(JSON)',
    oper_user       VARCHAR(50)         DEFAULT ''       COMMENT '操作人用户名',
    oper_ip         VARCHAR(128)        DEFAULT ''       COMMENT '操作IP',
    status          TINYINT             DEFAULT 1        COMMENT '状态: 0失败 1成功',
    error_msg       TEXT                                 COMMENT '错误消息',
    cost_time       BIGINT              DEFAULT 0        COMMENT '耗时(ms)',
    oper_time       DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_oper_time  (oper_time),
    INDEX idx_module     (module),
    INDEX idx_oper_user  (oper_user)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================================
-- 12. 种子数据
-- ============================================================

-- 默认部门（总部）
INSERT IGNORE INTO sys_dept (id, parent_id, ancestors, dept_name, dept_code, sort_order, status) VALUES
(1, 0, '0',     '产城总部',         'CCROOT', 0, 1),
(2, 1, '0,1',   '信息技术中心',     'IT',     1, 1),
(3, 1, '0,1',   '招商运营中心',     'MKT',    2, 1),
(4, 1, '0,1',   '财务管理中心',     'FIN',    3, 1),
(5, 1, '0,1',   '综合管理部',       'ADM',    4, 1);

-- 默认岗位
INSERT IGNORE INTO sys_post (id, post_code, post_name, sort_order, status) VALUES
(1, 'ADMIN',    '系统管理员',   1, 1),
(2, 'CEO',      '总经理',       2, 1),
(3, 'PM',       '项目经理',     3, 1),
(4, 'FIN',      '财务专员',     4, 1),
(5, 'MKT',      '招商专员',     5, 1);

-- 默认角色
INSERT IGNORE INTO sys_role (id, role_name, role_code, data_scope, sort_order) VALUES
(1, '超级管理员', 'SUPER_ADMIN', 1, 1),
(2, '系统管理员', 'SYS_ADMIN',  1, 2),
(3, '项目经理',  'PM',          3, 3),
(4, '招商专员',  'MKT',         4, 4),
(5, '财务专员',  'FIN',         4, 5);

-- 关联 admin 用户的角色（admin userId=1，在 init.sql 中已建）
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 默认菜单树（系统管理模块）
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order) VALUES
-- 系统管理目录
(100,  0,   '系统管理',   'M', '/sys',               '',                              '',                  'Setting',  99),
-- 用户管理
(101, 100,  '用户管理',   'C', '/sys/users',         'sys/user/index',                'sys:user:list',     'User',     1),
(1011, 101, '新增用户',   'F', '',                   '',                              'sys:user:create',   '',         1),
(1012, 101, '编辑用户',   'F', '',                   '',                              'sys:user:update',   '',         2),
(1013, 101, '删除用户',   'F', '',                   '',                              'sys:user:delete',   '',         3),
(1014, 101, '重置密码',   'F', '',                   '',                              'sys:user:resetPwd', '',         4),
-- 部门/机构管理
(102, 100,  '机构管理',   'C', '/sys/depts',         'sys/dept/index',                'sys:dept:list',     'Tree',     2),
(1021, 102, '新增部门',   'F', '',                   '',                              'sys:dept:create',   '',         1),
(1022, 102, '编辑部门',   'F', '',                   '',                              'sys:dept:update',   '',         2),
(1023, 102, '删除部门',   'F', '',                   '',                              'sys:dept:delete',   '',         3),
-- 岗位管理
(103, 100,  '岗位管理',   'C', '/sys/posts',         'sys/post/index',                'sys:post:list',     'Briefcase',3),
(1031, 103, '新增岗位',   'F', '',                   '',                              'sys:post:create',   '',         1),
(1032, 103, '编辑岗位',   'F', '',                   '',                              'sys:post:update',   '',         2),
(1033, 103, '删除岗位',   'F', '',                   '',                              'sys:post:delete',   '',         3),
-- 角色管理
(104, 100,  '角色管理',   'C', '/sys/roles',         'sys/role/index',                'sys:role:list',     'Key',      4),
(1041, 104, '新增角色',   'F', '',                   '',                              'sys:role:create',   '',         1),
(1042, 104, '编辑角色',   'F', '',                   '',                              'sys:role:update',   '',         2),
(1043, 104, '删除角色',   'F', '',                   '',                              'sys:role:delete',   '',         3),
(1044, 104, '分配菜单',   'F', '',                   '',                              'sys:role:grant',    '',         4),
-- 菜单管理
(105, 100,  '菜单管理',   'C', '/sys/menus',         'sys/menu/index',                'sys:menu:list',     'Menu',     5),
(1051, 105, '新增菜单',   'F', '',                   '',                              'sys:menu:create',   '',         1),
(1052, 105, '编辑菜单',   'F', '',                   '',                              'sys:menu:update',   '',         2),
(1053, 105, '删除菜单',   'F', '',                   '',                              'sys:menu:delete',   '',         3),
-- 业务字典
(106, 100,  '业务字典',   'C', '/sys/dict',          'sys/dict/index',                'sys:dict:list',     'Files',    6),
(1061, 106, '新增字典',   'F', '',                   '',                              'sys:dict:create',   '',         1),
(1062, 106, '编辑字典',   'F', '',                   '',                              'sys:dict:update',   '',         2),
(1063, 106, '删除字典',   'F', '',                   '',                              'sys:dict:delete',   '',         3),
-- 操作日志
(107, 100,  '操作日志',   'C', '/sys/logs',          'sys/log/index',                 'sys:log:list',      'Document', 7);

-- 超级管理员拥有所有菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- 默认字典类型
INSERT IGNORE INTO sys_dict_type (id, dict_name, dict_type, status) VALUES
(1,  '用户状态',     'sys_user_status',     1),
(2,  '部门状态',     'sys_dept_status',     1),
(3,  '岗位状态',     'sys_post_status',     1),
(4,  '角色状态',     'sys_role_status',     1),
(5,  '菜单类型',     'sys_menu_type',       1),
(6,  '数据范围',     'sys_data_scope',      1),
(7,  '业态类型',     'biz_format_type',     1),
(8,  '产权性质',     'biz_property_type',   1),
(9,  '经营类型',     'biz_business_type',   1),
(10, '运营状态',     'biz_operation_status',1),
(11, '合同类型',     'inv_contract_type',   1),
(12, '计租方案',     'inv_rent_scheme_type',1),
(13, '付款周期',     'inv_payment_cycle',   1);

-- 默认字典数据
INSERT IGNORE INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, sort_order) VALUES
-- 用户状态
('sys_user_status', '正常', '1', 'success', 1),
('sys_user_status', '停用', '0', 'danger',  2),
-- 部门状态
('sys_dept_status', '正常', '1', 'success', 1),
('sys_dept_status', '停用', '0', 'danger',  2),
-- 岗位状态
('sys_post_status', '正常', '1', 'success', 1),
('sys_post_status', '停用', '0', 'danger',  2),
-- 角色状态
('sys_role_status', '正常', '1', 'success', 1),
('sys_role_status', '停用', '0', 'danger',  2),
-- 菜单类型
('sys_menu_type', '目录', 'M', '',        1),
('sys_menu_type', '菜单', 'C', 'primary', 2),
('sys_menu_type', '按钮', 'F', 'warning', 3),
-- 数据范围
('sys_data_scope', '全部数据权限',     '1', '', 1),
('sys_data_scope', '自定义数据权限',   '2', '', 2),
('sys_data_scope', '本部门数据权限',   '3', '', 3),
('sys_data_scope', '本部门及以下',     '4', '', 4),
('sys_data_scope', '仅本人数据权限',   '5', '', 5),
-- 业态类型
('biz_format_type', '餐饮',   '1', '', 1),
('biz_format_type', '零售',   '2', '', 2),
('biz_format_type', '娱乐',   '3', '', 3),
('biz_format_type', '服务',   '4', '', 4),
('biz_format_type', '办公',   '5', '', 5),
('biz_format_type', '其他',   '9', '', 9),
-- 产权性质
('biz_property_type', '国有', '1', '', 1),
('biz_property_type', '集体', '2', '', 2),
('biz_property_type', '私有', '3', '', 3),
('biz_property_type', '其他', '4', '', 4),
-- 经营类型
('biz_business_type', '自持', '1', 'primary', 1),
('biz_business_type', '租赁', '2', 'success', 2),
('biz_business_type', '合作', '3', 'warning', 3),
-- 运营状态
('biz_operation_status', '筹备中', '0', 'info',    1),
('biz_operation_status', '已开业', '1', 'success', 2),
('biz_operation_status', '已停业', '2', 'danger',  3),
-- 合同类型
('inv_contract_type', '新签',   '1', 'primary', 1),
('inv_contract_type', '续签',   '2', 'success', 2),
('inv_contract_type', '转让',   '3', 'warning', 3),
-- 付款周期
('inv_payment_cycle', '月付',   '1', '', 1),
('inv_payment_cycle', '季付',   '3', '', 2),
('inv_payment_cycle', '半年付', '6', '', 3),
('inv_payment_cycle', '年付',   '12','', 4);

-- ============================================================
-- 13. 登录日志表 (sys_login_log)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_login_log (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username    VARCHAR(64)  NOT NULL                     COMMENT '用户名',
    ip_addr     VARCHAR(64)  DEFAULT ''                   COMMENT 'IP地址',
    browser     VARCHAR(100) DEFAULT ''                   COMMENT '浏览器',
    os          VARCHAR(100) DEFAULT ''                   COMMENT '操作系统',
    status      TINYINT      NOT NULL DEFAULT 0           COMMENT '状态: 0成功 1失败',
    msg         VARCHAR(255) DEFAULT ''                   COMMENT '消息/失败原因',
    login_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    INDEX idx_username   (username),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

SET FOREIGN_KEY_CHECKS = 1;

-- ─── TASK-SYS-05 补丁：角色自定义数据权限表 ─────────────────────────────────
CREATE TABLE IF NOT EXISTS sys_role_data (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    role_id     BIGINT UNSIGNED NOT NULL  COMMENT '角色ID',
    dept_id     BIGINT UNSIGNED NOT NULL  COMMENT '部门ID（自定义数据权限）',
    created_by  BIGINT UNSIGNED DEFAULT 0,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  BIGINT UNSIGNED DEFAULT 0,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted  TINYINT         NOT NULL DEFAULT 0,
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色自定义数据权限（部门）';
