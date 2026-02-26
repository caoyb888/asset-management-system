package com.asset.operation.change.service.impl;

import com.asset.common.exception.BizException;
import com.asset.operation.change.dto.*;
import com.asset.operation.change.entity.OprContractChange;
import com.asset.operation.change.entity.OprContractChangeDetail;
import com.asset.operation.change.entity.OprContractChangeType;
import com.asset.operation.change.mapper.OprContractChangeDetailMapper;
import com.asset.operation.change.mapper.OprContractChangeMapper;
import com.asset.operation.change.mapper.OprContractChangeSnapshotMapper;
import com.asset.operation.change.mapper.OprContractChangeTypeMapper;
import com.asset.operation.change.service.OprContractChangeService;
import com.asset.operation.common.enums.ChangeTypeCode;
import com.asset.operation.engine.ReceivableRecalculateEngine;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 合同变更 ServiceImpl
 * 实现变更单全生命周期：草稿→审批中→通过/驳回
 * 审批通过后触发 ReceivableRecalculateEngine 执行应收重算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OprContractChangeServiceImpl extends ServiceImpl<OprContractChangeMapper, OprContractChange>
        implements OprContractChangeService {

    private final OprContractChangeTypeMapper changeTypeMapper;
    private final OprContractChangeDetailMapper changeDetailMapper;
    private final OprContractChangeSnapshotMapper changeSnapshotMapper;
    private final ReceivableRecalculateEngine recalculateEngine;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // 查询
    // =========================================================================

    @Override
    public IPage<OprContractChange> pageQuery(ChangeQueryDTO query) {
        LambdaQueryWrapper<OprContractChange> wrapper = new LambdaQueryWrapper<OprContractChange>()
                .eq(query.getContractId() != null, OprContractChange::getContractId, query.getContractId())
                .eq(query.getLedgerId() != null, OprContractChange::getLedgerId, query.getLedgerId())
                .eq(query.getProjectId() != null, OprContractChange::getProjectId, query.getProjectId())
                .eq(query.getStatus() != null, OprContractChange::getStatus, query.getStatus())
                .like(query.getChangeCode() != null && !query.getChangeCode().isBlank(),
                        OprContractChange::getChangeCode, query.getChangeCode())
                .orderByDesc(OprContractChange::getCreatedAt);

        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;
        IPage<OprContractChange> result = page(new Page<>(pageNum, pageSize), wrapper);

        // 按变更类型筛选（从关联表过滤）
        if (query.getChangeTypeCode() != null && !query.getChangeTypeCode().isBlank()) {
            List<Long> matchedIds = getChangeIdsByTypeCode(query.getChangeTypeCode());
            result.setRecords(result.getRecords().stream()
                    .filter(c -> matchedIds.contains(c.getId()))
                    .collect(Collectors.toList()));
        }

        // 填充每条记录的变更类型列表（用于前端展示标签）
        result.getRecords().forEach(this::fillTypeCodes);

        return result;
    }

    @Override
    public ChangeDetailVO getDetailById(Long id) {
        OprContractChange change = getById(id);
        if (change == null) throw new BizException("变更单不存在，id=" + id);
        return buildDetailVO(change);
    }

    @Override
    public List<ChangeDetailVO> listHistory(Long contractId) {
        List<OprContractChange> changes = list(new LambdaQueryWrapper<OprContractChange>()
                .eq(OprContractChange::getContractId, contractId)
                .orderByDesc(OprContractChange::getCreatedAt));
        return changes.stream().map(this::buildDetailVO).collect(Collectors.toList());
    }

    // =========================================================================
    // 新增变更单
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ChangeCreateDTO dto) {
        if (dto.getChangeTypeCodes() == null || dto.getChangeTypeCodes().isEmpty()) {
            throw new BizException("变更类型不能为空");
        }

        // 1. 查询关联台账ID（若未传则从合同查找）
        Long ledgerId = dto.getLedgerId();
        if (ledgerId == null) {
            try {
                ledgerId = jdbcTemplate.queryForObject(
                        "SELECT id FROM opr_contract_ledger WHERE contract_id = ? AND is_deleted = 0 LIMIT 1",
                        Long.class, dto.getContractId());
            } catch (Exception ignored) {}
        }

        // 2. 查询项目ID
        Long projectId = null;
        try {
            projectId = jdbcTemplate.queryForObject(
                    "SELECT project_id FROM inv_lease_contract WHERE id = ? AND is_deleted = 0 LIMIT 1",
                    Long.class, dto.getContractId());
        } catch (Exception ignored) {}

        // 3. 生成变更单号
        String changeCode = genChangeCode();

        // 4. 保存变更主表
        OprContractChange change = new OprContractChange();
        change.setChangeCode(changeCode);
        change.setContractId(dto.getContractId());
        change.setLedgerId(ledgerId);
        change.setProjectId(projectId);
        change.setStatus(0);  // 草稿
        change.setEffectiveDate(dto.getEffectiveDate());
        change.setReason(dto.getReason());
        save(change);

        // 5. 保存变更类型关联记录
        saveChangeTypes(change.getId(), dto.getChangeTypeCodes());

        // 6. 保存变更字段明细（从 changeFields 构建）
        saveChangeDetails(change.getId(), dto.getChangeTypeCodes(), dto.getChangeFields());

        log.info("[合同变更] 变更单创建成功，changeId={}，changeCode={}", change.getId(), changeCode);
        return change.getId();
    }

    // =========================================================================
    // 编辑变更单（仅草稿/驳回可改）
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ChangeCreateDTO dto) {
        OprContractChange change = getById(id);
        if (change == null) throw new BizException("变更单不存在，id=" + id);
        if (change.getStatus() != 0 && change.getStatus() != 3) {
            throw new BizException("仅草稿或驳回状态的变更单可修改");
        }

        // 更新主表
        change.setEffectiveDate(dto.getEffectiveDate());
        change.setReason(dto.getReason());
        change.setStatus(0);  // 驳回后重新编辑变为草稿
        updateById(change);

        // 重建变更类型
        changeTypeMapper.delete(new LambdaQueryWrapper<OprContractChangeType>()
                .eq(OprContractChangeType::getChangeId, id));
        saveChangeTypes(id, dto.getChangeTypeCodes());

        // 重建字段明细
        changeDetailMapper.delete(new LambdaQueryWrapper<OprContractChangeDetail>()
                .eq(OprContractChangeDetail::getChangeId, id));
        saveChangeDetails(id, dto.getChangeTypeCodes(), dto.getChangeFields());

        log.info("[合同变更] 变更单更新，changeId={}", id);
    }

    // =========================================================================
    // 预览变更影响
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChangeImpactVO previewImpact(Long changeId) {
        OprContractChange change = getById(changeId);
        if (change == null) throw new BizException("变更单不存在，id=" + changeId);

        ChangeImpactVO vo = recalculateEngine.preview(change);

        // 将预览结果暂存到 impact_summary
        try {
            String json = objectMapper.writeValueAsString(vo);
            update(new LambdaUpdateWrapper<OprContractChange>()
                    .eq(OprContractChange::getId, changeId)
                    .set(OprContractChange::getImpactSummary,
                            objectMapper.readTree(json)));
        } catch (Exception e) {
            log.warn("[合同变更] 保存 impact_summary 失败：{}", e.getMessage());
        }

        return vo;
    }

    // =========================================================================
    // 提交审批
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(Long changeId) {
        OprContractChange change = getById(changeId);
        if (change == null) throw new BizException("变更单不存在，id=" + changeId);
        if (change.getStatus() != 0 && change.getStatus() != 3) {
            throw new BizException("仅草稿或驳回状态可提交审批");
        }

        // 模拟生成审批实例ID（对接真实 OA 时替换此处）
        String mockApprovalId = "APPROVAL-" + changeId + "-" + System.currentTimeMillis();

        update(new LambdaUpdateWrapper<OprContractChange>()
                .eq(OprContractChange::getId, changeId)
                .set(OprContractChange::getStatus, 1)           // 审批中
                .set(OprContractChange::getApprovalId, mockApprovalId));

        log.info("[合同变更] 变更单已提交审批，changeId={}，approvalId={}", changeId, mockApprovalId);
    }

    // =========================================================================
    // 审批回调
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalCallback(Long changeId, ApprovalCallbackDTO dto) {
        if (dto.getStatus() == null || (dto.getStatus() != 2 && dto.getStatus() != 3)) {
            throw new BizException("审批状态无效，必须为 2（通过）或 3（驳回）");
        }

        OprContractChange change = getById(changeId);
        if (change == null) throw new BizException("变更单不存在，id=" + changeId);
        if (change.getStatus() != 1) {
            throw new BizException("当前变更单状态不是审批中，无法回调");
        }

        // 更新状态
        update(new LambdaUpdateWrapper<OprContractChange>()
                .eq(OprContractChange::getId, changeId)
                .set(OprContractChange::getStatus, dto.getStatus()));

        if (dto.getStatus() == 2) {
            // 审批通过：触发应收重算
            log.info("[合同变更] 审批通过，触发应收重算，changeId={}", changeId);
            // 重新加载以获取最新数据
            change.setStatus(2);
            recalculateEngine.execute(change);
        } else {
            log.info("[合同变更] 审批驳回，changeId={}", changeId);
        }
    }

    // =========================================================================
    // 私有辅助方法
    // =========================================================================

    private ChangeDetailVO buildDetailVO(OprContractChange change) {
        ChangeDetailVO vo = new ChangeDetailVO();
        org.springframework.beans.BeanUtils.copyProperties(change, vo);
        vo.setStatusName(getStatusName(change.getStatus()));

        // 变更类型列表
        List<OprContractChangeType> types = changeTypeMapper.selectList(
                new LambdaQueryWrapper<OprContractChangeType>()
                        .eq(OprContractChangeType::getChangeId, change.getId())
                        .eq(OprContractChangeType::getIsDeleted, 0));
        List<String> codes = types.stream().map(OprContractChangeType::getChangeTypeCode).collect(Collectors.toList());
        List<String> names = codes.stream().map(c -> {
            ChangeTypeCode ctc = ChangeTypeCode.of(c);
            return ctc != null ? ctc.getDesc() : c;
        }).collect(Collectors.toList());
        vo.setChangeTypeCodes(codes);
        vo.setChangeTypeNames(names);

        // 字段明细
        List<OprContractChangeDetail> details = changeDetailMapper.selectList(
                new LambdaQueryWrapper<OprContractChangeDetail>()
                        .eq(OprContractChangeDetail::getChangeId, change.getId())
                        .eq(OprContractChangeDetail::getIsDeleted, 0)
                        .orderByAsc(OprContractChangeDetail::getId));
        vo.setDetails(details);

        // 关联合同信息
        if (change.getContractId() != null) {
            try {
                Map<String, Object> contract = jdbcTemplate.queryForMap(
                        "SELECT contract_code, contract_name, merchant_id FROM inv_lease_contract WHERE id = ? LIMIT 1",
                        change.getContractId());
                vo.setContractCode((String) contract.get("contract_code"));
                vo.setContractName((String) contract.get("contract_name"));
                Object merchantId = contract.get("merchant_id");
                if (merchantId != null) {
                    try {
                        String name = jdbcTemplate.queryForObject(
                                "SELECT merchant_name FROM biz_merchant WHERE id = ? LIMIT 1",
                                String.class, merchantId);
                        vo.setMerchantName(name);
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }

        // 项目名称
        if (change.getProjectId() != null) {
            try {
                String name = jdbcTemplate.queryForObject(
                        "SELECT project_name FROM biz_project WHERE id = ? LIMIT 1",
                        String.class, change.getProjectId());
                vo.setProjectName(name);
            } catch (Exception ignored) {}
        }

        return vo;
    }

    /** 填充变更记录的类型编码列表（用于列表展示） */
    private void fillTypeCodes(OprContractChange change) {
        List<String> codes = changeTypeMapper.selectList(new LambdaQueryWrapper<OprContractChangeType>()
                .eq(OprContractChangeType::getChangeId, change.getId())
                .eq(OprContractChangeType::getIsDeleted, 0))
                .stream().map(OprContractChangeType::getChangeTypeCode).collect(Collectors.toList());
        // 将 codes 暂存到 impactSummary（避免额外字段），实际前端从 VO 中取
        // 直接在列表 VO 中返回 codes，但主表没有该字段，需要调用方自行处理
        // 此处通过 transient 或 VO 解决，保持实体干净，调用方在 controller 中额外组装
    }

    private void saveChangeTypes(Long changeId, List<String> typeCodes) {
        for (String code : typeCodes) {
            OprContractChangeType type = new OprContractChangeType();
            type.setChangeId(changeId);
            type.setChangeTypeCode(code);
            changeTypeMapper.insert(type);
        }
    }

    /** 保存变更字段明细（从 changeFields Map 构建） */
    private void saveChangeDetails(Long changeId, List<String> typeCodes, Map<String, Object> changeFields) {
        if (changeFields == null || changeFields.isEmpty()) return;

        // 按变更类型定义字段映射
        Map<String, String[][]> typeFieldMap = buildTypeFieldMap();

        for (String typeCode : typeCodes) {
            String[][] fieldDefs = typeFieldMap.get(typeCode);
            if (fieldDefs == null) continue;
            for (String[] def : fieldDefs) {
                // def[0]=fieldName, def[1]=fieldLabel, def[2]=dataType
                String fieldName = def[0];
                String oldKey = "old_" + fieldName;
                Object newVal = changeFields.get(fieldName);
                Object oldVal = changeFields.get(oldKey);
                if (newVal == null && oldVal == null) continue;

                OprContractChangeDetail detail = new OprContractChangeDetail();
                detail.setChangeId(changeId);
                detail.setFieldName(fieldName);
                detail.setFieldLabel(def[1]);
                detail.setOldValue(oldVal != null ? oldVal.toString() : null);
                detail.setNewValue(newVal != null ? newVal.toString() : null);
                detail.setDataType(def[2]);
                changeDetailMapper.insert(detail);
            }
        }
    }

    /** 变更类型 → 字段定义映射 [fieldName, fieldLabel, dataType] */
    private Map<String, String[][]> buildTypeFieldMap() {
        Map<String, String[][]> m = new HashMap<>();
        m.put("RENT", new String[][]{
                {"newRentAmount", "租金金额（月）", "decimal"},
                {"oldRentAmount", "原租金金额", "decimal"}
        });
        m.put("FEE", new String[][]{
                {"newRentAmount", "费项单价", "decimal"},
                {"oldRentAmount", "原费项单价", "decimal"}
        });
        m.put("TERM", new String[][]{
                {"newContractEnd", "新合同到期日", "date"},
                {"newContractStart", "新合同开始日", "date"}
        });
        m.put("AREA", new String[][]{
                {"newRentArea", "新租赁面积（㎡）", "decimal"},
                {"oldRentArea", "原租赁面积", "decimal"}
        });
        m.put("BRAND", new String[][]{
                {"newBrandName", "新品牌名称", "string"}
        });
        m.put("TENANT", new String[][]{
                {"newMerchantName", "新商家名称", "string"}
        });
        m.put("COMPANY", new String[][]{
                {"newCompanyName", "新公司名称", "string"}
        });
        m.put("CLAUSE", new String[][]{
                {"clauseContent", "条款内容", "string"}
        });
        return m;
    }

    private List<Long> getChangeIdsByTypeCode(String typeCode) {
        return changeTypeMapper.selectList(new LambdaQueryWrapper<OprContractChangeType>()
                .eq(OprContractChangeType::getChangeTypeCode, typeCode)
                .eq(OprContractChangeType::getIsDeleted, 0))
                .stream().map(OprContractChangeType::getChangeId).collect(Collectors.toList());
    }

    /** 生成变更单号：BG + yyMMdd + 4位流水 */
    private String genChangeCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String prefix = "BG" + date;
        try {
            String max = jdbcTemplate.queryForObject(
                    "SELECT MAX(change_code) FROM opr_contract_change WHERE change_code LIKE ? AND is_deleted = 0",
                    String.class, prefix + "%");
            if (max != null && max.length() >= 10) {
                long seq = Long.parseLong(max.substring(8)) + 1;
                return prefix + String.format("%04d", seq);
            }
        } catch (Exception ignored) {}
        return prefix + "0001";
    }

    private String getStatusName(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "审批中";
            case 2 -> "已通过";
            case 3 -> "已驳回";
            default -> "";
        };
    }
}
