-- 任务7补丁：为 inv_rent_decomposition 追加 policy_snapshot 字段
-- 仅在字段不存在时执行（MySQL 8.0 不支持 IF NOT EXISTS 对列，用存储过程绕过）
SET @db = DATABASE();
SET @tbl = 'inv_rent_decomposition';
SET @col = 'policy_snapshot';
SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db AND TABLE_NAME = @tbl AND COLUMN_NAME = @col
    ),
    CONCAT('ALTER TABLE ', @tbl, ' ADD COLUMN policy_snapshot JSON NULL COMMENT ''租决政策关键参数快照(避免政策变更影响已有分解)'' AFTER approval_id'),
    'SELECT ''policy_snapshot column already exists, skip.'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
