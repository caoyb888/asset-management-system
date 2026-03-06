package com.asset.operation.ledger.service.impl;

import com.asset.common.exception.BizException;
import com.asset.operation.engine.ReceivablePlanGenerator;
import com.asset.operation.ledger.dto.AuditDTO;
import com.asset.operation.ledger.dto.LedgerDetailVO;
import com.asset.operation.ledger.dto.LedgerQueryDTO;
import com.asset.operation.ledger.dto.LedgerSelectorVO;
import com.asset.operation.ledger.dto.OneTimePaymentDTO;
import com.asset.operation.ledger.entity.OprContractLedger;
import com.asset.operation.ledger.entity.OprOneTimePayment;
import com.asset.operation.ledger.entity.OprReceivablePlan;
import com.asset.operation.ledger.mapper.OprContractLedgerMapper;
import com.asset.operation.ledger.mapper.OprOneTimePaymentMapper;
import com.asset.operation.ledger.mapper.OprReceivablePlanMapper;
import com.asset.operation.ledger.service.OprContractLedgerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 合同台账 Service 实现
 * 实现台账生命周期管理：生成→双签→应收生成→审核→推送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OprContractLedgerServiceImpl extends ServiceImpl<OprContractLedgerMapper, OprContractLedger>
        implements OprContractLedgerService {

    private final OprReceivablePlanMapper receivablePlanMapper;
    private final OprOneTimePaymentMapper oneTimePaymentMapper;
    private final ReceivablePlanGenerator receivablePlanGenerator;
    private final JdbcTemplate jdbcTemplate;

    /** 台账编号序列（应使用 Redis 自增，此处用 AtomicLong 作为简化实现） */
    private static final AtomicLong LEDGER_SEQ = new AtomicLong(1);

    // ====================================================
    // 查询
    // ====================================================

    @Override
    public IPage<OprContractLedger> pageQuery(LedgerQueryDTO query) {
        LambdaQueryWrapper<OprContractLedger> wrapper = new LambdaQueryWrapper<OprContractLedger>()
                .eq(query.getProjectId() != null, OprContractLedger::getProjectId, query.getProjectId())
                .eq(query.getContractType() != null, OprContractLedger::getContractType, query.getContractType())
                .eq(query.getStatus() != null, OprContractLedger::getStatus, query.getStatus())
                .eq(query.getAuditStatus() != null, OprContractLedger::getAuditStatus, query.getAuditStatus())
                .eq(query.getDoubleSignStatus() != null, OprContractLedger::getDoubleSignStatus, query.getDoubleSignStatus())
                .like(query.getLedgerCode() != null && !query.getLedgerCode().isBlank(),
                        OprContractLedger::getLedgerCode, query.getLedgerCode())
                .ge(query.getContractEndFrom() != null && !query.getContractEndFrom().isBlank(),
                        OprContractLedger::getContractEnd, query.getContractEndFrom())
                .le(query.getContractEndTo() != null && !query.getContractEndTo().isBlank(),
                        OprContractLedger::getContractEnd, query.getContractEndTo())
                .orderByDesc(OprContractLedger::getCreatedAt);

        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public LedgerDetailVO getDetailById(Long id) {
        OprContractLedger ledger = getById(id);
        if (ledger == null) {
            throw new BizException("台账不存在，id=" + id);
        }

        LedgerDetailVO vo = new LedgerDetailVO();
        // 复制基础字段
        org.springframework.beans.BeanUtils.copyProperties(ledger, vo);

        // 查询应收计划列表
        List<OprReceivablePlan> plans = receivablePlanMapper.selectList(
                new LambdaQueryWrapper<OprReceivablePlan>()
                        .eq(OprReceivablePlan::getLedgerId, id)
                        .ne(OprReceivablePlan::getStatus, 3) // 排除已作废
                        .orderByAsc(OprReceivablePlan::getDueDate)
        );
        vo.setReceivablePlans(plans);

        // 查询关联合同信息（跨模块，同库直接查）
        try {
            String contractSQL = """
                    SELECT c.contract_code, c.contract_name FROM inv_lease_contract c
                    WHERE c.id = ? AND c.is_deleted = 0 LIMIT 1
                    """;
            Map<String, Object> contractInfo = jdbcTemplate.queryForMap(contractSQL, ledger.getContractId());
            vo.setContractCode((String) contractInfo.get("contract_code"));
            vo.setContractName((String) contractInfo.get("contract_name"));
        } catch (Exception e) {
            log.warn("[台账详情] 查询合同信息失败，contractId={}", ledger.getContractId());
        }

        // 查询项目名称
        try {
            String projectSQL = "SELECT project_name FROM biz_project WHERE id = ? AND is_deleted = 0 LIMIT 1";
            String projectName = jdbcTemplate.queryForObject(projectSQL, String.class, ledger.getProjectId());
            vo.setProjectName(projectName);
        } catch (Exception ignored) {}

        // 查询商家名称
        if (ledger.getMerchantId() != null) {
            try {
                String merchantSQL = "SELECT merchant_name FROM biz_merchant WHERE id = ? AND is_deleted = 0 LIMIT 1";
                String name = jdbcTemplate.queryForObject(merchantSQL, String.class, ledger.getMerchantId());
                vo.setMerchantName(name);
            } catch (Exception ignored) {}
        }

        // 查询品牌名称
        if (ledger.getBrandId() != null) {
            try {
                String brandSQL = "SELECT brand_name_cn AS brand_name FROM biz_brand WHERE id = ? AND is_deleted = 0 LIMIT 1";
                String name = jdbcTemplate.queryForObject(brandSQL, String.class, ledger.getBrandId());
                vo.setBrandName(name);
            } catch (Exception ignored) {}
        }

        // 查询第一个商铺信息
        try {
            String shopSQL = """
                    SELECT s.shop_id, biz.shop_code FROM inv_lease_contract_shop s
                    JOIN biz_shop biz ON biz.id = s.shop_id
                    WHERE s.contract_id = ? AND s.is_deleted = 0 ORDER BY s.id LIMIT 1
                    """;
            Map<String, Object> shopInfo = jdbcTemplate.queryForMap(shopSQL, ledger.getContractId());
            vo.setShopId(((Number) shopInfo.get("shop_id")).longValue());
            vo.setShopCode((String) shopInfo.get("shop_code"));
        } catch (Exception ignored) {}

        // 填充枚举名称
        vo.setContractTypeName(getContractTypeName(ledger.getContractType()));
        vo.setDoubleSignStatusName(ledger.getDoubleSignStatus() != null && ledger.getDoubleSignStatus() == 1 ? "已双签" : "待双签");
        vo.setReceivableStatusName(getReceivableStatusName(ledger.getReceivableStatus()));
        vo.setAuditStatusName(getAuditStatusName(ledger.getAuditStatus()));
        vo.setStatusName(getLedgerStatusName(ledger.getStatus()));

        return vo;
    }

    // ====================================================
    // 台账生成（招商合同审批通过后触发）
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateFromContract(Long contractId) {
        log.info("[台账生成] 开始为招商合同创建台账，contractId={}", contractId);

        // 防止重复创建
        Long existing = baseMapper.selectCount(
                new LambdaQueryWrapper<OprContractLedger>()
                        .eq(OprContractLedger::getContractId, contractId)
        );
        if (existing > 0) {
            throw new BizException("台账已存在，请勿重复创建，contractId=" + contractId);
        }

        // 查询招商合同信息
        String contractSQL = """
                SELECT c.id, c.project_id, c.merchant_id, c.brand_id, c.contract_type,
                       c.contract_start, c.contract_end
                FROM inv_lease_contract c
                WHERE c.id = ? AND c.is_deleted = 0 LIMIT 1
                """;
        Map<String, Object> contractInfo;
        try {
            contractInfo = jdbcTemplate.queryForMap(contractSQL, contractId);
        } catch (Exception e) {
            throw new BizException("招商合同不存在，id=" + contractId);
        }

        // 生成台账
        OprContractLedger ledger = new OprContractLedger();
        ledger.setLedgerCode(genLedgerCode());
        ledger.setContractId(contractId);
        ledger.setProjectId(((Number) contractInfo.get("project_id")).longValue());
        if (contractInfo.get("merchant_id") != null) {
            ledger.setMerchantId(((Number) contractInfo.get("merchant_id")).longValue());
        }
        if (contractInfo.get("brand_id") != null) {
            ledger.setBrandId(((Number) contractInfo.get("brand_id")).longValue());
        }
        if (contractInfo.get("contract_type") != null) {
            ledger.setContractType(((Number) contractInfo.get("contract_type")).intValue());
        }
        ledger.setContractStart(toLocalDate(contractInfo.get("contract_start")));
        ledger.setContractEnd(toLocalDate(contractInfo.get("contract_end")));
        ledger.setDoubleSignStatus(0);   // 待双签
        ledger.setReceivableStatus(0);   // 未生成
        ledger.setAuditStatus(0);        // 待审核
        ledger.setStatus(0);             // 进行中

        save(ledger);
        log.info("[台账生成] 台账创建成功，ledgerId={}，ledgerCode={}", ledger.getId(), ledger.getLedgerCode());
        return ledger.getId();
    }

    // ====================================================
    // 双签确认
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDoubleSign(Long ledgerId) {
        OprContractLedger ledger = getAndValidate(ledgerId);
        if (ledger.getDoubleSignStatus() != null && ledger.getDoubleSignStatus() == 1) {
            throw new BizException("该台账已完成双签，请勿重复操作");
        }
        update(new LambdaUpdateWrapper<OprContractLedger>()
                .eq(OprContractLedger::getId, ledgerId)
                .set(OprContractLedger::getDoubleSignStatus, 1)
                .set(OprContractLedger::getDoubleSignDate, LocalDateTime.now())
        );
        log.info("[双签确认] 台账 {} 双签完成", ledgerId);
    }

    // ====================================================
    // 应收计划生成
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateReceivable(Long ledgerId) {
        getAndValidate(ledgerId);
        int count = receivablePlanGenerator.generate(ledgerId);
        // 更新台账应收状态为"已生成"
        update(new LambdaUpdateWrapper<OprContractLedger>()
                .eq(OprContractLedger::getId, ledgerId)
                .set(OprContractLedger::getReceivableStatus, 1)
        );
        return count;
    }

    // ====================================================
    // 审核
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long ledgerId, AuditDTO dto) {
        if (dto.getAuditStatus() == null || (dto.getAuditStatus() != 1 && dto.getAuditStatus() != 2)) {
            throw new BizException("审核状态无效，必须为 1（通过）或 2（驳回）");
        }
        OprContractLedger ledger = getAndValidate(ledgerId);
        if (ledger.getAuditStatus() != null && ledger.getAuditStatus() == 1) {
            throw new BizException("台账已通过审核，不可重复审核");
        }
        update(new LambdaUpdateWrapper<OprContractLedger>()
                .eq(OprContractLedger::getId, ledgerId)
                .set(OprContractLedger::getAuditStatus, dto.getAuditStatus())
        );
        log.info("[台账审核] 台账 {} 审核结果={}", ledgerId, dto.getAuditStatus() == 1 ? "通过" : "驳回");

        // 审核通过后触发应收推送
        if (dto.getAuditStatus() == 1 && ledger.getReceivableStatus() != null && ledger.getReceivableStatus() >= 1) {
            pushReceivable(ledgerId);
        }
    }

    // ====================================================
    // 应收推送
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pushReceivable(Long ledgerId) {
        OprContractLedger ledger = getAndValidate(ledgerId);
        if (ledger.getReceivableStatus() == null || ledger.getReceivableStatus() == 0) {
            throw new BizException("应收计划尚未生成，无法推送");
        }

        // 查询待推送应收计划
        List<OprReceivablePlan> plans = receivablePlanMapper.selectList(
                new LambdaQueryWrapper<OprReceivablePlan>()
                        .eq(OprReceivablePlan::getLedgerId, ledgerId)
                        .eq(OprReceivablePlan::getPushStatus, 0)
                        .ne(OprReceivablePlan::getStatus, 3)
        );

        if (plans.isEmpty()) {
            log.info("[应收推送] 台账 {} 无待推送应收，跳过", ledgerId);
            return;
        }

        // 为每条应收生成幂等键并更新推送状态
        // 实际生产中应通过 MQ 异步发送，此处同步处理（集成财务系统时替换）
        LocalDateTime now = LocalDateTime.now();
        for (OprReceivablePlan plan : plans) {
            String idempotentKey = "receivable_" + plan.getId() + "_" + plan.getVersion();
            receivablePlanMapper.update(null, new LambdaUpdateWrapper<OprReceivablePlan>()
                    .eq(OprReceivablePlan::getId, plan.getId())
                    .set(OprReceivablePlan::getPushIdempotentKey, idempotentKey)
                    .set(OprReceivablePlan::getPushStatus, 1)
                    .set(OprReceivablePlan::getPushTime, now)
            );
        }

        // 更新台账应收状态为"已推送"
        update(new LambdaUpdateWrapper<OprContractLedger>()
                .eq(OprContractLedger::getId, ledgerId)
                .set(OprContractLedger::getReceivableStatus, 2)
                .set(OprContractLedger::getPushTime, now)
        );
        log.info("[应收推送] 台账 {} 推送 {} 条应收计划完成", ledgerId, plans.size());
    }

    // ====================================================
    // 一次性首款
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOneTimePayment(Long ledgerId, OneTimePaymentDTO dto) {
        OprContractLedger ledger = getAndValidate(ledgerId);
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("首款金额必须大于0");
        }
        if (dto.getFeeItemId() == null) {
            throw new BizException("收款项目不能为空");
        }

        // 保存首款记录
        OprOneTimePayment payment = new OprOneTimePayment();
        payment.setLedgerId(ledgerId);
        payment.setContractId(ledger.getContractId());
        payment.setFeeItemId(dto.getFeeItemId());
        payment.setAmount(dto.getAmount());
        payment.setBillingStart(dto.getBillingStart());
        payment.setBillingEnd(dto.getBillingEnd());
        payment.setEntryType(dto.getEntryType() != null ? dto.getEntryType() : 1);
        payment.setRemark(dto.getRemark());
        oneTimePaymentMapper.insert(payment);

        // 为首款生成应收计划（source_type=4）
        String feeName = "首款";
        try {
            String feeSQL = "SELECT item_name FROM cfg_fee_item WHERE id = ? AND is_deleted = 0 LIMIT 1";
            feeName = jdbcTemplate.queryForObject(feeSQL, String.class, dto.getFeeItemId());
        } catch (Exception ignored) {}

        OprReceivablePlan plan = new OprReceivablePlan();
        plan.setLedgerId(ledgerId);
        plan.setContractId(ledger.getContractId());
        plan.setFeeItemId(dto.getFeeItemId());
        plan.setFeeName(feeName);
        plan.setBillingStart(dto.getBillingStart());
        plan.setBillingEnd(dto.getBillingEnd());
        plan.setDueDate(dto.getBillingStart() != null ? dto.getBillingStart() : LocalDate.now());
        plan.setAmount(dto.getAmount());
        plan.setReceivedAmount(BigDecimal.ZERO);
        plan.setStatus(0);
        plan.setPushStatus(0);
        plan.setSourceType(4);   // 一次性录入
        plan.setVersion(1);
        receivablePlanMapper.insert(plan);

        // 回填 receivable_id 到首款记录
        oneTimePaymentMapper.update(null, new LambdaUpdateWrapper<OprOneTimePayment>()
                .eq(OprOneTimePayment::getId, payment.getId())
                .set(OprOneTimePayment::getReceivableId, plan.getId())
        );

        log.info("[一次性首款] 台账 {} 录入首款成功，paymentId={}，receivableId={}", ledgerId, payment.getId(), plan.getId());
    }

    // ====================================================
    // 选择器搜索（供前端 ContractSelector 组件使用）
    // ====================================================

    @Override
    public List<LedgerSelectorVO> searchForSelector(String keyword, int pageSize) {
        // 先按台账编号或商家名模糊查台账ID列表
        LambdaQueryWrapper<OprContractLedger> wrapper = new LambdaQueryWrapper<OprContractLedger>()
                .eq(OprContractLedger::getStatus, 0) // 仅进行中台账
                .orderByDesc(OprContractLedger::getCreatedAt)
                .last("LIMIT " + Math.min(pageSize, 50));

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(OprContractLedger::getLedgerCode, keyword)
                    .or()
                    // merchantId 上做 LIKE 无意义，依赖后续关联查询过滤
                    .apply("1=1")
            );
        }

        List<OprContractLedger> ledgers = list(wrapper);
        if (ledgers.isEmpty()) {
            return List.of();
        }

        // 为每条台账补充关联信息
        List<LedgerSelectorVO> result = new java.util.ArrayList<>();
        for (OprContractLedger ledger : ledgers) {
            LedgerSelectorVO vo = new LedgerSelectorVO();
            vo.setId(ledger.getId());
            vo.setLedgerCode(ledger.getLedgerCode());
            vo.setContractStart(ledger.getContractStart());
            vo.setContractEnd(ledger.getContractEnd());
            vo.setStatus(ledger.getStatus());

            // 查合同编号
            try {
                String contractCode = jdbcTemplate.queryForObject(
                        "SELECT contract_code FROM inv_lease_contract WHERE id = ? AND is_deleted = 0 LIMIT 1",
                        String.class, ledger.getContractId());
                vo.setContractCode(contractCode);
            } catch (Exception ignored) {}

            // 查商家名称
            if (ledger.getMerchantId() != null) {
                try {
                    String merchantName = jdbcTemplate.queryForObject(
                            "SELECT merchant_name FROM biz_merchant WHERE id = ? AND is_deleted = 0 LIMIT 1",
                            String.class, ledger.getMerchantId());
                    vo.setMerchantName(merchantName);
                } catch (Exception ignored) {}
            }

            // 查第一个商铺编号
            try {
                String shopCode = jdbcTemplate.queryForObject(
                        """
                        SELECT biz.shop_code FROM inv_lease_contract_shop s
                        JOIN biz_shop biz ON biz.id = s.shop_id
                        WHERE s.contract_id = ? AND s.is_deleted = 0 ORDER BY s.id LIMIT 1
                        """,
                        String.class, ledger.getContractId());
                vo.setShopCode(shopCode);
            } catch (Exception ignored) {}

            // 关键字同时匹配商家名
            if (keyword != null && !keyword.isBlank()) {
                boolean matchCode = vo.getLedgerCode() != null && vo.getLedgerCode().contains(keyword);
                boolean matchMerchant = vo.getMerchantName() != null && vo.getMerchantName().contains(keyword);
                boolean matchContract = vo.getContractCode() != null && vo.getContractCode().contains(keyword);
                if (!matchCode && !matchMerchant && !matchContract) {
                    continue; // 跳过不匹配
                }
            }

            result.add(vo);
            if (result.size() >= pageSize) break;
        }
        return result;
    }

    // ====================================================
    // 私有辅助方法
    // ====================================================

    /** 校验台账存在并返回 */
    private OprContractLedger getAndValidate(Long ledgerId) {
        OprContractLedger ledger = getById(ledgerId);
        if (ledger == null) {
            throw new BizException("台账不存在，id=" + ledgerId);
        }
        return ledger;
    }

    /** 生成台账编号：TZ + yyMMdd + 6位序号 */
    private String genLedgerCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        // 查询当日最大序号
        String maxCodeSQL = """
                SELECT MAX(ledger_code) FROM opr_contract_ledger
                WHERE ledger_code LIKE ? AND is_deleted = 0
                """;
        try {
            String maxCode = jdbcTemplate.queryForObject(maxCodeSQL, String.class, "TZ" + date + "%");
            if (maxCode != null && maxCode.length() >= 14) {
                long seq = Long.parseLong(maxCode.substring(8)) + 1;
                return "TZ" + date + String.format("%06d", seq);
            }
        } catch (Exception ignored) {}
        return "TZ" + date + String.format("%06d", 1);
    }

    private LocalDate toLocalDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate ld) return ld;
        if (val instanceof java.sql.Date d) return d.toLocalDate();
        return LocalDate.parse(val.toString().substring(0, 10));
    }

    private String getContractTypeName(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "租赁合同";
            case 2 -> "联营合同";
            case 3 -> "临时合同";
            default -> "";
        };
    }

    private String getReceivableStatusName(Integer status) {
        if (status == null) return "未生成";
        return switch (status) {
            case 0 -> "未生成";
            case 1 -> "已生成";
            case 2 -> "已推送";
            default -> "";
        };
    }

    private String getAuditStatusName(Integer status) {
        if (status == null) return "待审核";
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已驳回";
            default -> "";
        };
    }

    private String getLedgerStatusName(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "进行中";
            case 1 -> "已完成";
            case 2 -> "已解约";
            default -> "";
        };
    }
}
