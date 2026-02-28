package com.asset.finance.voucher.service.impl;

import com.asset.common.exception.BizException;
import com.asset.finance.common.exception.FinBizException;
import com.asset.finance.common.exception.FinErrorCode;
import com.asset.finance.common.util.FinCodeGenerator;
import com.asset.finance.voucher.dto.VoucherCreateDTO;
import com.asset.finance.voucher.dto.VoucherDetailVO;
import com.asset.finance.voucher.dto.VoucherEntryDTO;
import com.asset.finance.voucher.dto.VoucherQueryDTO;
import com.asset.finance.voucher.entity.FinVoucher;
import com.asset.finance.voucher.entity.FinVoucherEntry;
import com.asset.finance.voucher.mapper.FinVoucherEntryMapper;
import com.asset.finance.voucher.mapper.FinVoucherMapper;
import com.asset.finance.voucher.service.FinVoucherService;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 凭证管理 ServiceImpl
 *
 * <p>凭证状态机：0(待审核) → audit() → 1(已审核) → upload() → 2(已上传)
 * <p>创建时校验借贷平衡：Σ debitAmount == Σ creditAmount，误差在 0.01 以内认为平衡。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinVoucherServiceImpl extends ServiceImpl<FinVoucherMapper, FinVoucher>
        implements FinVoucherService {

    private final FinVoucherEntryMapper entryMapper;
    private final FinCodeGenerator codeGenerator;
    private final JdbcTemplate jdbcTemplate;

    // 标准科目（简化，实际对接科目表）
    private static final String ACCOUNT_BANK     = "1002";
    private static final String ACCOUNT_BANK_NAME = "银行存款";
    private static final String ACCOUNT_AR       = "1122";
    private static final String ACCOUNT_AR_NAME  = "应收账款";

    // ─── 分页查询 ─────────────────────────────────────────────────────────────
    @Override
    public IPage<VoucherDetailVO> pageQuery(VoucherQueryDTO query) {
        LambdaQueryWrapper<FinVoucher> wrapper = new LambdaQueryWrapper<FinVoucher>()
                .like(StringUtils.hasText(query.getVoucherCode()),
                        FinVoucher::getVoucherCode, query.getVoucherCode())
                .eq(query.getProjectId() != null, FinVoucher::getProjectId, query.getProjectId())
                .eq(StringUtils.hasText(query.getAccountSet()),
                        FinVoucher::getAccountSet, query.getAccountSet())
                .eq(query.getPayType() != null, FinVoucher::getPayType, query.getPayType())
                .eq(query.getStatus() != null, FinVoucher::getStatus, query.getStatus())
                .ge(query.getDateFrom() != null, FinVoucher::getVoucherDate, query.getDateFrom())
                .le(query.getDateTo() != null, FinVoucher::getVoucherDate, query.getDateTo())
                .orderByDesc(FinVoucher::getId);

        IPage<FinVoucher> page = baseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        // 转换为 VO（列表不带分录）
        return page.convert(v -> toDetailVO(v, false));
    }

    // ─── 详情（含分录）────────────────────────────────────────────────────────
    @Override
    public VoucherDetailVO getDetail(Long id) {
        FinVoucher voucher = baseMapper.selectById(id);
        if (voucher == null) throw new BizException("凭证不存在: id=" + id);
        return toDetailVO(voucher, true);
    }

    // ─── 手动创建凭证 ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVoucher(VoucherCreateDTO dto) {
        // 借贷平衡校验
        checkDebitCreditBalance(dto.getEntries());

        FinVoucher voucher = new FinVoucher();
        voucher.setVoucherCode(codeGenerator.voucherCode());
        voucher.setProjectId(dto.getProjectId());
        voucher.setAccountSet(StringUtils.hasText(dto.getAccountSet()) ? dto.getAccountSet() : "默认账套");
        voucher.setPayType(dto.getPayType());
        voucher.setVoucherDate(dto.getVoucherDate() != null ? dto.getVoucherDate() : LocalDate.now());
        voucher.setRemark(dto.getRemark());
        voucher.setStatus(0);

        BigDecimal totalDebit  = dto.getEntries().stream()
                .map(VoucherEntryDTO::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = dto.getEntries().stream()
                .map(VoucherEntryDTO::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);

        baseMapper.insert(voucher);

        // 插入分录
        for (VoucherEntryDTO e : dto.getEntries()) {
            entryMapper.insert(toEntryEntity(voucher.getId(), e));
        }

        log.info("[凭证] 创建 {}，项目={}，借贷={}", voucher.getVoucherCode(), dto.getProjectId(), totalDebit);
        return voucher.getId();
    }

    // ─── 从收款单自动生成收款凭证 ─────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateFromReceipt(Long receiptId) {
        // 查收款单
        Map<String, Object> receipt;
        try {
            receipt = jdbcTemplate.queryForMap(
                    "SELECT id, receipt_code, project_id, total_amount, receipt_date, bank_name, payer_name " +
                    "FROM fin_receipt WHERE id=? AND is_deleted=0 LIMIT 1", receiptId);
        } catch (Exception e) {
            throw new BizException("收款单不存在: id=" + receiptId);
        }

        Long projectId   = toLong(receipt.get("project_id"));
        BigDecimal amount = (BigDecimal) receipt.get("total_amount");
        LocalDate receiptDate = ((java.sql.Date) receipt.get("receipt_date")).toLocalDate();
        String receiptCode = (String) receipt.get("receipt_code");

        // 标准两条分录：借 银行存款 / 贷 应收账款
        FinVoucher voucher = new FinVoucher();
        voucher.setVoucherCode(codeGenerator.voucherCode());
        voucher.setProjectId(projectId);
        voucher.setAccountSet("默认账套");
        voucher.setPayType(1);  // 收款
        voucher.setVoucherDate(receiptDate);
        voucher.setTotalDebit(amount);
        voucher.setTotalCredit(amount);
        voucher.setStatus(0);
        voucher.setRemark("收款单 " + receiptCode + " 自动生成");
        baseMapper.insert(voucher);

        // 借：银行存款
        FinVoucherEntry debitEntry = new FinVoucherEntry();
        debitEntry.setVoucherId(voucher.getId());
        debitEntry.setSourceType(1);
        debitEntry.setSourceId(receiptId);
        debitEntry.setAccountCode(ACCOUNT_BANK);
        debitEntry.setAccountName(ACCOUNT_BANK_NAME);
        debitEntry.setDebitAmount(amount);
        debitEntry.setCreditAmount(BigDecimal.ZERO);
        debitEntry.setSummary("收款 - " + receiptCode);
        entryMapper.insert(debitEntry);

        // 贷：应收账款
        FinVoucherEntry creditEntry = new FinVoucherEntry();
        creditEntry.setVoucherId(voucher.getId());
        creditEntry.setSourceType(1);
        creditEntry.setSourceId(receiptId);
        creditEntry.setAccountCode(ACCOUNT_AR);
        creditEntry.setAccountName(ACCOUNT_AR_NAME);
        creditEntry.setDebitAmount(BigDecimal.ZERO);
        creditEntry.setCreditAmount(amount);
        creditEntry.setSummary("核销应收 - " + receiptCode);
        entryMapper.insert(creditEntry);

        log.info("[凭证] 从收款单 {} 自动生成凭证 {}，金额={}", receiptCode, voucher.getVoucherCode(), amount);
        return voucher.getId();
    }

    // ─── 审核（status: 0 → 1） ────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id) {
        FinVoucher voucher = getOrThrow(id);
        if (voucher.getStatus() != 0) {
            throw new BizException("凭证状态不是待审核，当前状态=" + voucher.getStatus());
        }
        boolean ok = update(new LambdaUpdateWrapper<FinVoucher>()
                .eq(FinVoucher::getId, id)
                .eq(FinVoucher::getStatus, 0)
                .set(FinVoucher::getStatus, 1));
        if (!ok) throw new FinBizException(FinErrorCode.FIN_5001, "凭证审核失败，请重试");
        log.info("[凭证] 审核通过 id={}", id);
    }

    // ─── 上传（status: 1 → 2） ────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upload(Long id) {
        FinVoucher voucher = getOrThrow(id);
        if (voucher.getStatus() == 2) {
            throw new FinBizException(FinErrorCode.FIN_4007, "凭证已上传，不可重复上传");
        }
        if (voucher.getStatus() != 1) {
            throw new BizException("凭证尚未审核，不能上传");
        }

        // 模拟上传到外部财务系统
        log.info("[凭证] 模拟上传凭证 {} 到财务系统", voucher.getVoucherCode());

        boolean ok = update(new LambdaUpdateWrapper<FinVoucher>()
                .eq(FinVoucher::getId, id)
                .eq(FinVoucher::getStatus, 1)
                .set(FinVoucher::getStatus, 2)
                .set(FinVoucher::getUploadTime, LocalDateTime.now()));
        if (!ok) throw new FinBizException(FinErrorCode.FIN_5001, "凭证上传失败，请重试");
        log.info("[凭证] 上传成功 id={}", id);
    }

    // ─── 删除凭证（status=0 才允许）─────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVoucher(Long id) {
        FinVoucher voucher = getOrThrow(id);
        if (voucher.getStatus() != 0) {
            throw new BizException("只有待审核状态的凭证才能删除");
        }
        // 逻辑删除主表
        removeById(id);
        // 逻辑删除所有分录
        entryMapper.delete(new LambdaQueryWrapper<FinVoucherEntry>()
                .eq(FinVoucherEntry::getVoucherId, id));
        log.info("[凭证] 删除凭证 id={}", id);
    }

    // ─── 私有辅助 ─────────────────────────────────────────────────────────────

    private FinVoucher getOrThrow(Long id) {
        FinVoucher voucher = baseMapper.selectById(id);
        if (voucher == null) throw new BizException("凭证不存在: id=" + id);
        return voucher;
    }

    /** 借贷平衡校验（误差 ≤ 0.01 认为平衡） */
    private void checkDebitCreditBalance(List<VoucherEntryDTO> entries) {
        BigDecimal sumDebit  = entries.stream().map(VoucherEntryDTO::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumCredit = entries.stream().map(VoucherEntryDTO::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sumDebit.subtract(sumCredit).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new FinBizException(FinErrorCode.FIN_4006,
                    String.format("借方合计 %.2f ≠ 贷方合计 %.2f，借贷不平衡", sumDebit, sumCredit));
        }
    }

    private FinVoucherEntry toEntryEntity(Long voucherId, VoucherEntryDTO dto) {
        FinVoucherEntry entry = new FinVoucherEntry();
        entry.setVoucherId(voucherId);
        entry.setSourceType(dto.getSourceType());
        entry.setSourceId(dto.getSourceId());
        entry.setAccountCode(dto.getAccountCode());
        entry.setAccountName(dto.getAccountName());
        entry.setDebitAmount(dto.getDebitAmount() != null ? dto.getDebitAmount() : BigDecimal.ZERO);
        entry.setCreditAmount(dto.getCreditAmount() != null ? dto.getCreditAmount() : BigDecimal.ZERO);
        entry.setSummary(dto.getSummary());
        return entry;
    }

    private VoucherDetailVO toDetailVO(FinVoucher v, boolean withEntries) {
        VoucherDetailVO vo = new VoucherDetailVO();
        vo.setId(v.getId());
        vo.setVoucherCode(v.getVoucherCode());
        vo.setProjectId(v.getProjectId());
        vo.setAccountSet(v.getAccountSet());
        vo.setPayType(v.getPayType());
        vo.setPayTypeName(v.getPayType() != null ? (v.getPayType() == 1 ? "收款" : "付款") : null);
        vo.setVoucherDate(v.getVoucherDate());
        vo.setTotalDebit(v.getTotalDebit());
        vo.setTotalCredit(v.getTotalCredit());
        vo.setStatus(v.getStatus());
        vo.setStatusName(statusName(v.getStatus()));
        vo.setUploadTime(v.getUploadTime());
        vo.setRemark(v.getRemark());
        vo.setCreateTime(v.getCreatedAt());
        vo.setUpdateTime(v.getUpdatedAt());

        // 补充项目名称
        try {
            if (v.getProjectId() != null) {
                vo.setProjectName(jdbcTemplate.queryForObject(
                        "SELECT project_name FROM biz_project WHERE id=? AND is_deleted=0 LIMIT 1",
                        String.class, v.getProjectId()));
            }
        } catch (Exception e) {
            log.debug("[凭证VO] 项目名称查询异常：{}", e.getMessage());
        }

        // 查分录
        if (withEntries) {
            List<FinVoucherEntry> entries = entryMapper.selectByVoucherId(v.getId());
            vo.setEntries(entries);
        }
        return vo;
    }

    private String statusName(Integer status) {
        if (status == null) return null;
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已审核";
            case 2 -> "已上传";
            default -> "未知";
        };
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}
