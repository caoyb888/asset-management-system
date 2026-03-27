-- ============================================================
-- 工作流模块测试数据
-- 覆盖场景：18条流程实例 + 对应审批记录
-- 创建日期：2026-03-26
-- ============================================================
SET NAMES utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;

-- ============================================================
-- 一、wf_process_instance 测试数据（ID 10001~10018）
-- ============================================================

-- 10001: INV_INTENTION/91003，已通过，1级审批（amount=50000 < 10万），admin发起+审批
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10001, 'inv_intention_approval', 'WF-MOCK-10001', 'INV_INTENTION', 91003,
   '意向客户审批-91003（普通）',
   1, '系统管理员', 90001, NULL, NULL,
   2, '审核通过，符合招商要求', 0, '{"amount": 50000}',
   '2026-03-01 09:00:00', '2026-03-01 11:30:00', 9000000,
   1, '2026-03-01 09:00:00', 1, '2026-03-01 11:30:00', 0);

-- 10002: INV_INTENTION/91002，审批中，待部门领导(90001)，紧急，amount=45000
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10002, 'inv_intention_approval', 'WF-MOCK-10002', 'INV_INTENTION', 91002,
   '意向客户审批-91002（紧急）',
   1, '系统管理员', 90001, 90001, '部门领导审批',
   1, NULL, 1, '{"amount": 45000}',
   '2026-03-05 10:00:00', NULL, NULL,
   1, '2026-03-05 10:00:00', 1, '2026-03-05 10:00:00', 0);

-- 10003: INV_INTENTION/91001，已驳回，部门领导驳回，amount=30000
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10003, 'inv_intention_approval', 'WF-MOCK-10003', 'INV_INTENTION', 91001,
   '意向客户审批-91001（已驳回）',
   1, '系统管理员', 90001, NULL, NULL,
   3, '客户资质不符合要求，驳回申请', 0, '{"amount": 30000}',
   '2026-03-06 09:00:00', '2026-03-06 14:20:00', 19200000,
   1, '2026-03-06 09:00:00', 90001, '2026-03-06 14:20:00', 0);

-- 10004: INV_OPENING/91016，已通过，紧急，amount=0，90001发起+审批
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10004, 'inv_opening_approval', 'WF-MOCK-10004', 'INV_OPENING', 91016,
   '开业审批-91016（紧急）',
   90001, '测试经理-商业', 90001, NULL, NULL,
   2, '开业条件已具备，审核通过', 1, '{"amount": 0}',
   '2026-03-08 08:30:00', '2026-03-08 10:00:00', 5400000,
   90001, '2026-03-08 08:30:00', 90001, '2026-03-08 10:00:00', 0);

-- 10005: INV_OPENING/91003，已驳回，amount=0
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10005, 'inv_opening_approval', 'WF-MOCK-10005', 'INV_OPENING', 91003,
   '开业审批-91003（已驳回）',
   1, '系统管理员', 90001, NULL, NULL,
   3, '开业材料不齐全，驳回', 0, '{"amount": 0}',
   '2026-03-09 09:00:00', '2026-03-09 16:00:00', 25200000,
   1, '2026-03-09 09:00:00', 90001, '2026-03-09 16:00:00', 0);

-- 10006: INV_RENT_DECOMP/91002，审批中，部门领导已通过，待副总裁(90002)，amount=150000（2级）
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10006, 'inv_rent_decomp_approval', 'WF-MOCK-10006', 'INV_RENT_DECOMP', 91002,
   '租金分解审批-91002（待副总裁）',
   1, '系统管理员', 90001, 90002, '副总裁审批',
   1, NULL, 0, '{"amount": 150000}',
   '2026-03-10 09:00:00', NULL, NULL,
   1, '2026-03-10 09:00:00', 90001, '2026-03-10 11:00:00', 0);

-- 10007: INV_RENT_DECOMP/91001，已通过，2级审批（10万≤120000<50万），90001发起
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10007, 'inv_rent_decomp_approval', 'WF-MOCK-10007', 'INV_RENT_DECOMP', 91001,
   '租金分解审批-91001（已通过，2级）',
   90001, '测试经理-商业', 90001, NULL, NULL,
   2, '租金分解方案合理，审核通过', 0, '{"amount": 120000}',
   '2026-03-11 09:00:00', '2026-03-12 15:30:00', 109800000,
   90001, '2026-03-11 09:00:00', 90002, '2026-03-12 15:30:00', 0);

-- 10008: OPR_CONTRACT_CHANGE/92001，审批中，部门领导已通过，待副总裁(90002)，紧急，amount=200000
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10008, 'opr_contract_change_approval', 'WF-MOCK-10008', 'OPR_CONTRACT_CHANGE', 92001,
   '合同变更审批-92001（待副总裁，紧急）',
   90002, '营运专员-商业', 90002, 90002, '副总裁审批',
   1, NULL, 1, '{"amount": 200000}',
   '2026-03-12 10:00:00', NULL, NULL,
   90002, '2026-03-12 10:00:00', 90001, '2026-03-12 14:00:00', 0);

-- 10009: OPR_CONTRACT_CHANGE/92002，已通过，1级审批（80000<10万），90001发起
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10009, 'opr_contract_change_approval', 'WF-MOCK-10009', 'OPR_CONTRACT_CHANGE', 92002,
   '合同变更审批-92002（已通过，1级）',
   90001, '测试经理-商业', 90001, NULL, NULL,
   2, '变更内容合规，审核通过', 0, '{"amount": 80000}',
   '2026-03-13 09:00:00', '2026-03-13 16:00:00', 25200000,
   90001, '2026-03-13 09:00:00', 90001, '2026-03-13 16:00:00', 0);

-- 10010: OPR_CONTRACT_CHANGE/92003，已通过，3级审批（600000≥50万），加急，admin发起
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10010, 'opr_contract_change_approval', 'WF-MOCK-10010', 'OPR_CONTRACT_CHANGE', 92003,
   '合同变更审批-92003（已通过，3级，加急）',
   1, '系统管理员', 90002, NULL, NULL,
   2, '大额合同变更，三级审批通过', 2, '{"amount": 600000}',
   '2026-03-14 08:00:00', '2026-03-16 17:00:00', 219600000,
   1, '2026-03-14 08:00:00', 1, '2026-03-16 17:00:00', 0);

-- 10011: OPR_TERMINATION/92002，审批中，部门领导+副总裁已通过，待总经理(admin=1)，加急，amount=550000
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10011, 'opr_termination_approval', 'WF-MOCK-10011', 'OPR_TERMINATION', 92002,
   '合同终止审批-92002（待总经理，加急）',
   90002, '营运专员-商业', 90001, 1, '总经理审批',
   1, NULL, 2, '{"amount": 550000}',
   '2026-03-15 09:00:00', NULL, NULL,
   90002, '2026-03-15 09:00:00', 90002, '2026-03-17 11:00:00', 0);

-- 10012: OPR_TERMINATION/92003，已通过，1级审批（50000<10万）
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10012, 'opr_termination_approval', 'WF-MOCK-10012', 'OPR_TERMINATION', 92003,
   '合同终止审批-92003（已通过，1级）',
   90001, '测试经理-商业', 90002, NULL, NULL,
   2, '合同终止条件满足，审核通过', 0, '{"amount": 50000}',
   '2026-03-16 09:00:00', '2026-03-16 14:00:00', 18000000,
   90001, '2026-03-16 09:00:00', 90001, '2026-03-16 14:00:00', 0);

-- 10013: OPR_TERMINATION/92001，已撤回，admin撤回，amount=20000
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10013, 'opr_termination_approval', 'WF-MOCK-10013', 'OPR_TERMINATION', 92001,
   '合同终止审批-92001（已撤回）',
   1, '系统管理员', 90001, NULL, NULL,
   4, '发起人主动撤回申请', 0, '{"amount": 20000}',
   '2026-03-17 09:00:00', '2026-03-17 10:30:00', 5400000,
   1, '2026-03-17 09:00:00', 1, '2026-03-17 10:30:00', 0);

-- 10014: FIN_WRITE_OFF/93001，审批中，待部门领导(90001)，amount=5000
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10014, 'fin_write_off_approval', 'WF-MOCK-10014', 'FIN_WRITE_OFF', 93001,
   '核销申请-93001（待部门领导）',
   90002, '营运专员-商业', 90001, 90001, '部门领导审批',
   1, NULL, 0, '{"amount": 5000}',
   '2026-03-18 09:00:00', NULL, NULL,
   90002, '2026-03-18 09:00:00', 90002, '2026-03-18 09:00:00', 0);

-- 10015: FIN_WRITE_OFF/93003，已通过，1级审批（8000<10万）
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10015, 'fin_write_off_approval', 'WF-MOCK-10015', 'FIN_WRITE_OFF', 93003,
   '核销申请-93003（已通过，1级）',
   90001, '测试经理-商业', 90001, NULL, NULL,
   2, '核销条件已满足，审核通过', 0, '{"amount": 8000}',
   '2026-03-19 09:00:00', '2026-03-19 15:00:00', 21600000,
   90001, '2026-03-19 09:00:00', 90001, '2026-03-19 15:00:00', 0);

-- 10016: FIN_WRITE_OFF/93002，已作废（管理员作废），amount=3000
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10016, 'fin_write_off_approval', 'WF-MOCK-10016', 'FIN_WRITE_OFF', 93002,
   '核销申请-93002（已作废）',
   90002, '营运专员-商业', 90001, NULL, NULL,
   5, '数据有误，管理员作废该流程', 0, '{"amount": 3000}',
   '2026-03-20 09:00:00', '2026-03-20 11:00:00', 7200000,
   90002, '2026-03-20 09:00:00', 1, '2026-03-20 11:00:00', 0);

-- 10017: FIN_DEDUCTION/93001，审批中，待部门领导(90001)，紧急，amount=2000
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10017, 'fin_deduction_approval', 'WF-MOCK-10017', 'FIN_DEDUCTION', 93001,
   '应收扣减审批-93001（待部门领导，紧急）',
   90002, '营运专员-商业', 90001, 90001, '部门领导审批',
   1, NULL, 1, '{"amount": 2000}',
   '2026-03-21 09:00:00', NULL, NULL,
   90002, '2026-03-21 09:00:00', 90002, '2026-03-21 09:00:00', 0);

-- 10018: FIN_ADJUSTMENT/93001，已通过，1级审批（3000<10万）
INSERT IGNORE INTO wf_process_instance
  (id, process_key, flowable_instance_id, business_type, business_id, title,
   initiator_id, initiator_name, project_id, current_assignee_id, current_node_name,
   status, result_comment, priority, variables_json,
   started_at, finished_at, duration_ms,
   created_by, created_at, updated_by, updated_at, is_deleted)
VALUES
  (10018, 'fin_adjustment_approval', 'WF-MOCK-10018', 'FIN_ADJUSTMENT', 93001,
   '应收调整审批-93001（已通过，1级）',
   1, '系统管理员', 90001, NULL, NULL,
   2, '调整金额合规，审核通过', 0, '{"amount": 3000}',
   '2026-03-22 09:00:00', '2026-03-22 14:30:00', 19800000,
   1, '2026-03-22 09:00:00', 90001, '2026-03-22 14:30:00', 0);


-- ============================================================
-- 二、wf_approval_record 测试数据
-- ============================================================

-- 10001: admin通过，部门领导节点（1级审批已完成）
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10001, 'task-10001-01', '部门领导审批', 1,
   1, '系统管理员', 1, '符合招商要求，通过', NULL, 9000000, '2026-03-01 11:30:00');

-- 10003: 90001驳回，部门领导节点
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10003, 'task-10003-01', '部门领导审批', 1,
   90001, '测试经理-商业', 2, '客户资质不符合要求，驳回申请', NULL, 19200000, '2026-03-06 14:20:00');

-- 10004: 90001通过，部门领导节点（开业审批完成）
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10004, 'task-10004-01', '部门领导审批', 1,
   90001, '测试经理-商业', 1, '开业条件已具备，审核通过', NULL, 5400000, '2026-03-08 10:00:00');

-- 10005: 90001驳回，部门领导节点（开业审批驳回）
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10005, 'task-10005-01', '部门领导审批', 1,
   90001, '测试经理-商业', 2, '开业材料不齐全，驳回', NULL, 25200000, '2026-03-09 16:00:00');

-- 10006: 90001通过，部门领导节点（等待副总裁）
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10006, 'task-10006-01', '部门领导审批', 1,
   90001, '测试经理-商业', 1, '租金分解方案初步可行，提交副总裁复核', NULL, 7200000, '2026-03-10 11:00:00');

-- 10007: 2条审批记录（2级审批完成）
-- 90001通过部门领导
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10007, 'task-10007-01', '部门领导审批', 1,
   90001, '测试经理-商业', 1, '方案合理，同意提交', NULL, 36000000, '2026-03-12 09:00:00');
-- 90002通过副总裁
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10007, 'task-10007-02', '副总裁审批', 2,
   90002, '营运专员-商业', 1, '租金分解方案合理，审核通过', NULL, 23400000, '2026-03-12 15:30:00');

-- 10008: 90001通过，部门领导节点（等待副总裁，紧急）
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10008, 'task-10008-01', '部门领导审批', 1,
   90001, '测试经理-商业', 1, '合同变更内容合规，提交副总裁审批', NULL, 14400000, '2026-03-12 14:00:00');

-- 10009: 90001通过，部门领导节点（1级审批完成）
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10009, 'task-10009-01', '部门领导审批', 1,
   90001, '测试经理-商业', 1, '变更内容合规，审核通过', NULL, 25200000, '2026-03-13 16:00:00');

-- 10010: 3条审批记录（3级审批完成，加急）
-- 90001通过部门领导
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10010, 'task-10010-01', '部门领导审批', 1,
   90001, '测试经理-商业', 1, '大额合同变更，方案可行，提交上级审批', NULL, 57600000, '2026-03-14 16:00:00');
-- 90002通过副总裁
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10010, 'task-10010-02', '副总裁审批', 2,
   90002, '营运专员-商业', 1, '合同变更条款合规，同意提交总经理最终审批', NULL, 64800000, '2026-03-15 14:00:00');
-- admin通过总经理
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10010, 'task-10010-03', '总经理审批', 3,
   1, '系统管理员', 1, '大额合同变更审核通过', NULL, 54000000, '2026-03-16 17:00:00');

-- 10011: 2条审批记录（等待总经理，加急）
-- 90001通过部门领导
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10011, 'task-10011-01', '部门领导审批', 1,
   90001, '测试经理-商业', 1, '合同终止材料齐全，提交副总裁审批', NULL, 36000000, '2026-03-15 19:00:00');
-- 90002通过副总裁
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10011, 'task-10011-02', '副总裁审批', 2,
   90002, '营运专员-商业', 1, '大额合同终止，同意提交总经理最终决策', NULL, 57600000, '2026-03-17 11:00:00');

-- 10012: 90001通过，部门领导节点（1级审批完成）
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10012, 'task-10012-01', '部门领导审批', 1,
   90001, '测试经理-商业', 1, '合同终止条件满足，审核通过', NULL, 18000000, '2026-03-16 14:00:00');

-- 10013: admin撤回（action=5）
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10013, 'task-10013-01', '部门领导审批', 1,
   1, '系统管理员', 5, '发起人主动撤回申请', NULL, 5400000, '2026-03-17 10:30:00');

-- 10015: 90001通过，部门领导节点（1级核销审批完成）
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10015, 'task-10015-01', '部门领导审批', 1,
   90001, '测试经理-商业', 1, '核销条件已满足，审核通过', NULL, 21600000, '2026-03-19 15:00:00');

-- 10018: 90001通过，部门领导节点（1级调整审批完成）
INSERT IGNORE INTO wf_approval_record
  (instance_id, flowable_task_id, node_name, node_order,
   approver_id, approver_name, action, comment, attachment_urls, duration_ms, created_at)
VALUES
  (10018, 'task-10018-01', '部门领导审批', 1,
   90001, '测试经理-商业', 1, '调整金额合规，审核通过', NULL, 19800000, '2026-03-22 14:30:00');


-- ============================================================
-- 三、更新业务表的 approval_id 字段
-- ============================================================

-- inv_intention
UPDATE inv_intention SET approval_id = 'WF-MOCK-10001' WHERE id = 91003;
UPDATE inv_intention SET approval_id = 'WF-MOCK-10002' WHERE id = 91002;
UPDATE inv_intention SET approval_id = 'WF-MOCK-10003' WHERE id = 91001;

-- inv_opening_approval
UPDATE inv_opening_approval SET approval_id = 'WF-MOCK-10004' WHERE id = 91016;
UPDATE inv_opening_approval SET approval_id = 'WF-MOCK-10005' WHERE id = 91003;

-- inv_rent_decomposition
UPDATE inv_rent_decomposition SET approval_id = 'WF-MOCK-10006' WHERE id = 91002;
UPDATE inv_rent_decomposition SET approval_id = 'WF-MOCK-10007' WHERE id = 91001;

-- opr_contract_change
UPDATE opr_contract_change SET approval_id = 'WF-MOCK-10008' WHERE id = 92001;
UPDATE opr_contract_change SET approval_id = 'WF-MOCK-10009' WHERE id = 92002;
UPDATE opr_contract_change SET approval_id = 'WF-MOCK-10010' WHERE id = 92003;

-- opr_contract_termination
UPDATE opr_contract_termination SET approval_id = 'WF-MOCK-10011' WHERE id = 92002;
UPDATE opr_contract_termination SET approval_id = 'WF-MOCK-10012' WHERE id = 92003;
UPDATE opr_contract_termination SET approval_id = 'WF-MOCK-10013' WHERE id = 92001;

-- fin_write_off
UPDATE fin_write_off SET approval_id = 'WF-MOCK-10014' WHERE id = 93001;
UPDATE fin_write_off SET approval_id = 'WF-MOCK-10015' WHERE id = 93003;
UPDATE fin_write_off SET approval_id = 'WF-MOCK-10016' WHERE id = 93002;

-- fin_receivable_deduction
UPDATE fin_receivable_deduction SET approval_id = 'WF-MOCK-10017' WHERE id = 93001;

-- fin_receivable_adjustment
UPDATE fin_receivable_adjustment SET approval_id = 'WF-MOCK-10018' WHERE id = 93001;
