-- =============================================================
-- 资产管理系统 - 数据库初始化脚本
-- =============================================================

-- 业务库
CREATE DATABASE IF NOT EXISTS asset_db DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Nacos配置库
CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- XXL-Job调度库
CREATE DATABASE IF NOT EXISTS xxl_job DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Flowable工作流库
CREATE DATABASE IF NOT EXISTS asset_flowable DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 授权
GRANT ALL PRIVILEGES ON asset_db.* TO 'asset'@'%';
GRANT ALL PRIVILEGES ON nacos_config.* TO 'asset'@'%';
GRANT ALL PRIVILEGES ON xxl_job.* TO 'asset'@'%';
GRANT ALL PRIVILEGES ON asset_flowable.* TO 'asset'@'%';
FLUSH PRIVILEGES;
