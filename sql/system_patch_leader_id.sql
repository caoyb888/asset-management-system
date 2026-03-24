-- 为 sys_dept 添加部门负责人用户ID字段
-- TASK-VF-05: 审批人自动解析
ALTER TABLE sys_dept
    ADD COLUMN leader_id BIGINT NULL COMMENT '部门负责人用户ID（关联 sys_user.id）' AFTER leader;
