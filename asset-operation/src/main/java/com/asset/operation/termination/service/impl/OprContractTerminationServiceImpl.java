package com.asset.operation.termination.service.impl;

import com.asset.common.exception.BizException;
import com.asset.operation.change.dto.ApprovalCallbackDTO;
import com.asset.operation.common.enums.TerminationType;
import com.asset.operation.engine.TerminationSettlementEngine;
import com.asset.operation.termination.dto.TerminationCreateDTO;
import com.asset.operation.termination.dto.TerminationDetailVO;
import com.asset.operation.termination.dto.TerminationQueryDTO;
import com.asset.operation.termination.entity.OprContractTermination;
import com.asset.operation.termination.entity.OprTerminationSettlement;
import com.asset.operation.termination.mapper.OprContractTerminationMapper;
import com.asset.operation.termination.mapper.OprTerminationSettlementMapper;
import com.asset.operation.termination.service.OprContractTerminationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 合同解约 ServiceImpl
 * 实现解约单全生命周期：草稿→审批中→已生效/驳回
 * 审批通过后触发 TerminationSettlementEngine.execute() 事务性多表联动
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OprContractTerminationServiceImpl
        extends ServiceImpl<OprContractTerminationMapper, OprContractTermination>
        implements OprContractTerminationService {

    private final OprTerminationSettlementMapper settlementMapper;
    private final TerminationSettlementEngine settlementEngine;
    private final JdbcTemplate jdbcTemplate;

    // =========================================================================
    // 查询
    // =========================================================================

    @Override
    public IPage<TerminationDetailVO> pageQuery(TerminationQueryDTO query) {
        LambdaQueryWrapper<OprContractTermination> wrapper = new LambdaQueryWrapper<OprContractTermination>()
                .eq(query.getContractId() != null, OprContractTermination::getContractId, query.getContractId())
                .eq(query.getProjectId() != null, OprContractTermination::getProjectId, query.getProjectId())
                .eq(query.getTerminationType() != null, OprContractTermination::getTerminationType, query.getTerminationType())
                .eq(query.getStatus() != null, OprContractTermination::getStatus, query.getStatus())
                .like(query.getTerminationCode() != null && !query.getTerminationCode().isBlank(),
                        OprContractTermination::getTerminationCode, query.getTerminationCode())
                .orderByDesc(OprContractTermination::getCreatedAt);

        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;

        IPage<OprContractTermination> entityPage = page(new Page<>(pageNum, pageSize), wrapper);

        // 转换为 VO（列表不含清算明细，仅主表字段+冗余展示字段）
        IPage<TerminationDetailVO> voPage = entityPage.convert(t -> buildDetailVO(t, false));
        return voPage;
    }

    @Override
    public TerminationDetailVO getDetailById(Long id) {
        OprContractTermination t = getById(id);
        if (t == null) throw new BizException("解约单不存在，id=" + id);
        return buildDetailVO(t, true);
    }

    // =========================================================================
    // 新增解约单
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TerminationCreateDTO dto) {
        if (dto.getContractId() == null) throw new BizException("合同ID不能为空");
        if (dto.getTerminationType() == null) throw new BizException("解约类型不能为空");
        if (dto.getTerminationDate() == null) throw new BizException("解约日期不能为空");
        if (dto.getTerminationType() == 3 && dto.getNewContractId() == null) {
            throw new BizException("重签解约必须填写新合同ID");
        }

        // 检查是否已有进行中的解约单
        long existCount = count(new LambdaQueryWrapper<OprContractTermination>()
                .eq(OprContractTermination::getContractId, dto.getContractId())
                .in(OprContractTermination::getStatus, 0, 1)   // 草稿或审批中
                .eq(OprContractTermination::getIsDeleted, 0));
        if (existCount > 0) throw new BizException("该合同已有进行中的解约单，请勿重复提交");

        // 查询台账ID
        Long ledgerId = dto.getLedgerId();
        if (ledgerId == null) {
            try {
                ledgerId = jdbcTemplate.queryForObject(
                        "SELECT id FROM opr_contract_ledger WHERE contract_id=? AND is_deleted=0 LIMIT 1",
                        Long.class, dto.getContractId());
            } catch (Exception ignored) {}
        }

        // 查询项目ID、商家ID、品牌ID、商铺ID（从合同主表）
        Long projectId = null, merchantId = null, brandId = null, shopId = null;
        try {
            var row = jdbcTemplate.queryForMap(
                    "SELECT project_id, merchant_id, brand_id FROM inv_lease_contract WHERE id=? AND is_deleted=0 LIMIT 1",
                    dto.getContractId());
            projectId = toLong(row.get("project_id"));
            merchantId = toLong(row.get("merchant_id"));
            brandId = toLong(row.get("brand_id"));
        } catch (Exception ignored) {}

        // 查第一个商铺
        if (dto.getContractId() != null) {
            try {
                shopId = jdbcTemplate.queryForObject(
                        "SELECT shop_id FROM inv_lease_contract_shop WHERE contract_id=? AND is_deleted=0 LIMIT 1",
                        Long.class, dto.getContractId());
            } catch (Exception ignored) {}
        }

        // 生成解约单号
        String terminationCode = genTerminationCode();

        OprContractTermination termination = new OprContractTermination();
        termination.setTerminationCode(terminationCode);
        termination.setContractId(dto.getContractId());
        termination.setLedgerId(ledgerId);
        termination.setProjectId(projectId);
        termination.setMerchantId(merchantId);
        termination.setBrandId(brandId);
        termination.setShopId(shopId);
        termination.setTerminationType(dto.getTerminationType());
        termination.setTerminationDate(dto.getTerminationDate());
        termination.setReason(dto.getReason());
        termination.setNewContractId(dto.getNewContractId());
        termination.setPenaltyAmount(dto.getPenaltyRate());  // 暂存 penaltyRate 到 penaltyAmount，引擎计算后覆盖
        termination.setStatus(0);  // 草稿
        save(termination);

        log.info("[合同解约] 解约单创建成功，id={}，code={}", termination.getId(), terminationCode);
        return termination.getId();
    }

    // =========================================================================
    // 编辑解约单
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TerminationCreateDTO dto) {
        OprContractTermination t = getById(id);
        if (t == null) throw new BizException("解约单不存在，id=" + id);
        if (t.getStatus() != 0 && t.getStatus() != 3) {
            throw new BizException("仅草稿或驳回状态的解约单可修改");
        }

        t.setTerminationType(dto.getTerminationType());
        t.setTerminationDate(dto.getTerminationDate());
        t.setReason(dto.getReason());
        t.setNewContractId(dto.getNewContractId());
        t.setPenaltyAmount(dto.getPenaltyRate());
        t.setStatus(0);  // 驳回后重新编辑恢复草稿
        // 清空已有清算数据，等待重新计算
        t.setSettlementAmount(null);
        t.setUnsettledAmount(null);
        updateById(t);

        // 删除旧清算明细
        settlementMapper.delete(new LambdaQueryWrapper<OprTerminationSettlement>()
                .eq(OprTerminationSettlement::getTerminationId, id));

        log.info("[合同解约] 解约单更新，id={}", id);
    }

    // =========================================================================
    // 清算计算
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateSettlement(Long id) {
        OprContractTermination t = getById(id);
        if (t == null) throw new BizException("解约单不存在，id=" + id);
        if (t.getStatus() != 0 && t.getStatus() != 3) {
            throw new BizException("仅草稿或驳回状态可触发清算计算");
        }
        settlementEngine.calculateSettlement(id);
    }

    // =========================================================================
    // 审批流程
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(Long id) {
        OprContractTermination t = getById(id);
        if (t == null) throw new BizException("解约单不存在，id=" + id);
        if (t.getStatus() != 0 && t.getStatus() != 3) {
            throw new BizException("仅草稿或驳回状态可提交审批");
        }
        if (t.getSettlementAmount() == null) {
            throw new BizException("请先计算清算金额再提交审批");
        }

        String mockApprovalId = "TERM-APPROVAL-" + id + "-" + System.currentTimeMillis();
        update(new LambdaUpdateWrapper<OprContractTermination>()
                .eq(OprContractTermination::getId, id)
                .set(OprContractTermination::getStatus, 1)
                .set(OprContractTermination::getApprovalId, mockApprovalId));

        log.info("[合同解约] 已提交审批，id={}，approvalId={}", id, mockApprovalId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalCallback(Long id, ApprovalCallbackDTO dto) {
        if (dto.getStatus() == null || (dto.getStatus() != 2 && dto.getStatus() != 3)) {
            throw new BizException("审批状态无效，必须为 2（通过）或 3（驳回）");
        }

        OprContractTermination t = getById(id);
        if (t == null) throw new BizException("解约单不存在，id=" + id);
        if (t.getStatus() != 1) throw new BizException("当前状态不是审批中，无法回调");

        if (dto.getStatus() == 2) {
            // 审批通过：执行解约（引擎内部将状态改为已生效）
            log.info("[合同解约] 审批通过，执行解约，id={}", id);
            settlementEngine.execute(id);
        } else {
            // 驳回：回退为驳回状态
            update(new LambdaUpdateWrapper<OprContractTermination>()
                    .eq(OprContractTermination::getId, id)
                    .set(OprContractTermination::getStatus, 3));
            log.info("[合同解约] 审批驳回，id={}", id);
        }
    }

    // =========================================================================
    // 私有辅助方法
    // =========================================================================

    /** 构建详情 VO */
    private TerminationDetailVO buildDetailVO(OprContractTermination t, boolean withSettlements) {
        TerminationDetailVO vo = new TerminationDetailVO();
        BeanUtils.copyProperties(t, vo);
        vo.setStatusName(getStatusName(t.getStatus()));

        TerminationType type = TerminationType.of(t.getTerminationType() != null ? t.getTerminationType() : 0);
        vo.setTerminationTypeName(type != null ? type.getDesc() : "");

        // 合同信息
        if (t.getContractId() != null) {
            try {
                var row = jdbcTemplate.queryForMap(
                        "SELECT contract_code, contract_name FROM inv_lease_contract WHERE id=? LIMIT 1",
                        t.getContractId());
                vo.setContractCode((String) row.get("contract_code"));
                vo.setContractName((String) row.get("contract_name"));
            } catch (Exception ignored) {}
        }

        // 商家名称
        if (t.getMerchantId() != null) {
            try {
                String name = jdbcTemplate.queryForObject(
                        "SELECT merchant_name FROM biz_merchant WHERE id=? LIMIT 1",
                        String.class, t.getMerchantId());
                vo.setMerchantName(name);
            } catch (Exception ignored) {}
        }

        // 项目名称
        if (t.getProjectId() != null) {
            try {
                String name = jdbcTemplate.queryForObject(
                        "SELECT project_name FROM biz_project WHERE id=? LIMIT 1",
                        String.class, t.getProjectId());
                vo.setProjectName(name);
            } catch (Exception ignored) {}
        }

        // 商铺编号
        if (t.getShopId() != null) {
            try {
                String code = jdbcTemplate.queryForObject(
                        "SELECT shop_code FROM biz_shop WHERE id=? LIMIT 1",
                        String.class, t.getShopId());
                vo.setShopCode(code);
            } catch (Exception ignored) {}
        }

        // 清算明细（详情页才加载）
        if (withSettlements) {
            List<OprTerminationSettlement> settlements = settlementMapper.selectList(
                    new LambdaQueryWrapper<OprTerminationSettlement>()
                            .eq(OprTerminationSettlement::getTerminationId, t.getId())
                            .eq(OprTerminationSettlement::getIsDeleted, 0)
                            .orderByAsc(OprTerminationSettlement::getId));
            vo.setSettlements(settlements);
        }

        return vo;
    }

    /** 生成解约单号：JY + yyMMdd + 4位流水 */
    private String genTerminationCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String prefix = "JY" + date;
        try {
            String max = jdbcTemplate.queryForObject(
                    "SELECT MAX(termination_code) FROM opr_contract_termination WHERE termination_code LIKE ? AND is_deleted=0",
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
            case 2 -> "已生效";
            case 3 -> "已驳回";
            default -> "";
        };
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }
}
