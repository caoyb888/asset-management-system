-- ====================================================================
-- 资产管理系统 - V1.0.0 初始化数据库
-- ====================================================================

-- ----------------------------
-- 1. 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
    tenant_id       BIGINT          DEFAULT 0               COMMENT '租户ID',
    username        VARCHAR(50)     NOT NULL                 COMMENT '用户名',
    password        VARCHAR(200)    NOT NULL                 COMMENT '密码(SM3加密)',
    real_name       VARCHAR(50)     DEFAULT ''               COMMENT '真实姓名',
    phone           VARCHAR(20)     DEFAULT ''               COMMENT '手机号(SM4加密)',
    email           VARCHAR(100)    DEFAULT ''               COMMENT '邮箱',
    avatar          VARCHAR(500)    DEFAULT ''               COMMENT '头像',
    dept_id         BIGINT          DEFAULT NULL             COMMENT '部门ID',
    status          TINYINT         DEFAULT 1                COMMENT '状态(1正常 0禁用)',
    login_ip        VARCHAR(128)    DEFAULT ''               COMMENT '最后登录IP',
    login_time      DATETIME        DEFAULT NULL             COMMENT '最后登录时间',
    create_by       VARCHAR(50)     DEFAULT ''               COMMENT '创建者',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       VARCHAR(50)     DEFAULT ''               COMMENT '更新者',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0                COMMENT '删除标记(0正常 1删除)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username, tenant_id),
    KEY idx_tenant (tenant_id),
    KEY idx_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ----------------------------
-- 2. 角色表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '角色ID',
    tenant_id       BIGINT          DEFAULT 0               COMMENT '租户ID',
    role_name       VARCHAR(50)     NOT NULL                 COMMENT '角色名称',
    role_code       VARCHAR(50)     NOT NULL                 COMMENT '角色编码',
    data_scope      TINYINT         DEFAULT 1                COMMENT '数据权限(1全部 2自定义 3本部门 4本部门及以下 5本人)',
    sort_order      INT             DEFAULT 0                COMMENT '排序',
    status          TINYINT         DEFAULT 1                COMMENT '状态',
    remark          VARCHAR(500)    DEFAULT ''               COMMENT '备注',
    create_by       VARCHAR(50)     DEFAULT ''               COMMENT '创建者',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(50)     DEFAULT '',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- ----------------------------
-- 3. 菜单/权限表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_menu (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '菜单ID',
    parent_id       BIGINT          DEFAULT 0               COMMENT '父菜单ID',
    menu_name       VARCHAR(100)    NOT NULL                 COMMENT '菜单名称',
    menu_type       CHAR(1)         NOT NULL                 COMMENT '类型(M目录 C菜单 F按钮)',
    path            VARCHAR(200)    DEFAULT ''               COMMENT '路由地址',
    component       VARCHAR(255)    DEFAULT ''               COMMENT '组件路径',
    perms           VARCHAR(200)    DEFAULT ''               COMMENT '权限标识',
    icon            VARCHAR(100)    DEFAULT ''               COMMENT '图标',
    sort_order      INT             DEFAULT 0                COMMENT '排序',
    visible         TINYINT         DEFAULT 1                COMMENT '是否可见',
    status          TINYINT         DEFAULT 1                COMMENT '状态',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单权限表';

-- ----------------------------
-- 4. 用户角色关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ----------------------------
-- 5. 角色菜单关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    menu_id         BIGINT          NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ----------------------------
-- 6. 字典类型表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    dict_name       VARCHAR(100)    NOT NULL COMMENT '字典名称',
    dict_type       VARCHAR(100)    NOT NULL COMMENT '字典类型',
    status          TINYINT         DEFAULT 1,
    remark          VARCHAR(500)    DEFAULT '',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- ----------------------------
-- 7. 字典数据表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_dict_data (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    dict_type       VARCHAR(100)    NOT NULL COMMENT '字典类型',
    dict_label      VARCHAR(100)    NOT NULL COMMENT '字典标签',
    dict_value      VARCHAR(100)    NOT NULL COMMENT '字典值',
    sort_order      INT             DEFAULT 0,
    status          TINYINT         DEFAULT 1,
    remark          VARCHAR(500)    DEFAULT '',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- ----------------------------
-- 8. 操作日志表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_oper_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    module          VARCHAR(50)     DEFAULT '' COMMENT '模块',
    biz_type        VARCHAR(50)     DEFAULT '' COMMENT '业务类型',
    method          VARCHAR(200)    DEFAULT '' COMMENT '方法名',
    request_method  VARCHAR(10)     DEFAULT '' COMMENT 'HTTP方法',
    request_url     VARCHAR(500)    DEFAULT '' COMMENT '请求URL',
    request_param   TEXT                       COMMENT '请求参数',
    response_result TEXT                       COMMENT '响应结果',
    oper_user       VARCHAR(50)     DEFAULT '' COMMENT '操作人',
    oper_ip         VARCHAR(128)    DEFAULT '' COMMENT '操作IP',
    status          TINYINT         DEFAULT 1  COMMENT '状态(1成功 0失败)',
    error_msg       TEXT                       COMMENT '错误消息',
    cost_time       BIGINT          DEFAULT 0  COMMENT '耗时(ms)',
    oper_time       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_oper_time (oper_time),
    KEY idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ----------------------------
-- 初始数据: 超级管理员
-- ----------------------------
INSERT INTO sys_user (id, username, password, real_name, status) VALUES
(1, 'admin', '$SM3$salt$hash_placeholder', '超级管理员', 1);

INSERT INTO sys_role (id, role_name, role_code, data_scope) VALUES
(1, '超级管理员', 'admin', 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
