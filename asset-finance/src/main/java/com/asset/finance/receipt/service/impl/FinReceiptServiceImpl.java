package com.asset.finance.receipt.service.impl;

import com.asset.finance.common.exception.FinBizException;
import com.asset.finance.common.exception.FinErrorCode;
import com.asset.finance.common.util.FinCodeGenerator;
import com.asset.finance.receipt.dto.ReceiptCreateDTO;
import com.asset.finance.receipt.dto.ReceiptDetailItemDTO;
import com.asset.finance.receipt.dto.ReceiptDetailVO;
import com.asset.finance.receipt.dto.ReceiptQueryDTO;
import com.asset.finance.receipt.entity.FinReceipt;
import com.asset.finance.receipt.entity.FinReceiptDetail;
import com.asset.finance.receipt.mapper.FinReceiptDetailMapper;
import com.asset.finance.receipt.mapper.FinReceiptMapper;
import com.asset.finance.receipt.service.FinReceiptService;
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
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinReceiptServiceImpl extends ServiceImpl<FinReceiptMapper, FinReceipt>
        implements FinReceiptService {

    private final FinReceiptDetailMapper detailMapper;
    private final FinCodeGenerator codeGenerator;
    private final JdbcTemplate jdbcTemplate;

    // ─── 分页查询 ───────────────────────────────────────────────────────────
    @Override
    public IPage<ReceiptDetailVO> pageQuery(ReceiptQueryDTO query) {
        LambdaQueryWrapper<FinReceipt> w = new LambdaQueryWrapper<FinReceipt>()
                .eq(query.getContractId() != null, FinReceipt::getContractId, query.getContractId())
                .eq(query.getMerchantId() != null, FinReceipt::getMerchantId, query.getMerchantId())
                .eq(query.getProjectId() != null, FinReceipt::getProjectId, query.getProjectId())
                .eq(query.getStatus() != null, FinReceipt::getStatus, query.getStatus())
                .eq(query.getIsUnnamed() != null, FinReceipt::getIsUnnamed, query.getIsUnnamed())
                .eq(query.getPaymentMethod() != null, FinReceipt::getPaymentMethod, query.getPaymentMethod())
                .ge(query.getReceiptDateFrom() != null, FinReceipt::getReceiptDate, query.getReceiptDateFrom())
                .le(query.getReceiptDateTo() != null, FinReceipt::getReceiptDate, query.getReceiptDateTo())
                .like(query.getReceiptCode() != null && !query.getReceiptCode().isBlank(),
                        FinReceipt::getReceiptCode, query.getReceiptCode())
                .orderByDesc(FinReceipt::getReceiptDate);

        IPage<FinReceipt> entityPage = page(
                new Page<>(query.getPageNum(), query.getPageSize()), w);
        return entityPage.convert(this::toDetailVO);
    }

    // ─── 详情查询 ───────────────────────────────────────────────────────────
    @Override
    public ReceiptDetailVO getDetailById(Long id) {
        FinReceipt receipt = getById(id);
        if (receipt == null) {
            throw new FinBizException(FinErrorCode.FIN_5001, "收款单不存在，id=" + id);
        }
        ReceiptDetailVO vo = toDetailVO(receipt);
        // 查询拆分明细
        vo.setDetails(detailMapper.selectList(
                new LambdaQueryWrapper<FinReceiptDetail>()
                        .eq(FinReceiptDetail::getReceiptId, id)));
        return vo;
    }

    // ─── 新增收款单 ──────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ReceiptCreateDTO dto) {
        // 1. 校验拆分明细合计
        validateDetailSum(dto);

        // 2. 构建收款单主记录
        FinReceipt receipt = new FinReceipt();
        fillFromDTO(receipt, dto);
        receipt.setReceiptCode(codeGenerator.receiptCode());
        receipt.setStatus(0);
        receipt.setWriteOffAmount(BigDecimal.ZERO);
        receipt.setPrepayAmount(BigDecimal.ZERO);

        // 3. 若有合同，自动带出项目ID和商家ID
        if (dto.getContractId() != null) {
            fillContractInfo(receipt, dto.getContractId());
        }

        save(receipt);

        // 4. 保存拆分明细
        saveDetails(receipt.getId(), dto.getTotalAmount(), dto.getDetails());

        log.info("[收款单] 新增成功 receiptCode={}, totalAmount={}", receipt.getReceiptCode(), receipt.getTotalAmount());
        return receipt.getId();
    }

    // ─── 编辑收款单 ──────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ReceiptCreateDTO dto) {
        FinReceipt receipt = getById(id);
        if (receipt == null) {
            throw new FinBizException(FinErrorCode.FIN_5001, "收款单不存在，id=" + id);
        }
        // 仅待核销状态可编辑
        if (!Integer.valueOf(0).equals(receipt.getStatus())) {
            throw new FinBizException(FinErrorCode.FIN_4003);
        }

        validateDetailSum(dto);
        fillFromDTO(receipt, dto);
        if (dto.getContractId() != null) {
            fillContractInfo(receipt, dto.getContractId());
        }
        updateById(receipt);

        // 重建拆分明细：先逻辑删除旧明细，再插入新明细
        detailMapper.delete(new LambdaQueryWrapper<FinReceiptDetail>()
                .eq(FinReceiptDetail::getReceiptId, id));
        saveDetails(id, dto.getTotalAmount(), dto.getDetails());

        log.info("[收款单] 编辑成功 id={}", id);
    }

    // ─── 作废收款单 ──────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, String reason) {
        FinReceipt receipt = getById(id);
        if (receipt == null) {
            throw new FinBizException(FinErrorCode.FIN_5001, "收款单不存在，id=" + id);
        }
        // 仅 status=0（待核销）可作废
        if (!Integer.valueOf(0).equals(receipt.getStatus())) {
            throw new FinBizException(FinErrorCode.FIN_4003);
        }

        update(new LambdaUpdateWrapper<FinReceipt>()
                .eq(FinReceipt::getId, id)
                .set(FinReceipt::getStatus, 3));

        log.info("[收款单] 作废成功 id={}, reason={}", id, reason);
    }

    // ─── 未名款项归名 ────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bind(Long id, Long contractId) {
        FinReceipt receipt = getById(id);
        if (receipt == null) {
            throw new FinBizException(FinErrorCode.FIN_5001, "收款单不存在，id=" + id);
        }
        if (!Integer.valueOf(1).equals(receipt.getIsUnnamed())) {
            throw new FinBizException(FinErrorCode.FIN_5001, "该收款单不是未名款项");
        }

        // 绑定合同，自动带出项目/商家信息，并取消未名标记
        LambdaUpdateWrapper<FinReceipt> w = new LambdaUpdateWrapper<FinReceipt>()
                .eq(FinReceipt::getId, id)
                .set(FinReceipt::getContractId, contractId)
                .set(FinReceipt::getIsUnnamed, 0);

        try {
            Map<String, Object> c = jdbcTemplate.queryForMap(
                    "SELECT project_id, merchant_id FROM inv_lease_contract WHERE id=? LIMIT 1", contractId);
            w.set(FinReceipt::getProjectId, toLong(c.get("project_id")));
            w.set(FinReceipt::getMerchantId, toLong(c.get("merchant_id")));
        } catch (Exception e) {
            log.warn("[收款单归名] 未查到合同信息 contractId={}", contractId);
        }
        update(w);

        log.info("[收款单] 归名成功 id={}, contractId={}", id, contractId);
    }

    // ─── 私有辅助 ────────────────────────────────────────────────────────────

    /** 校验拆分明细合计与总金额一致 */
    private void validateDetailSum(ReceiptCreateDTO dto) {
        if (CollectionUtils.isEmpty(dto.getDetails())) {
            return; // 无明细时跳过（后续 saveDetails 会自动创建全额明细）
        }
        BigDecimal detailSum = dto.getDetails().stream()
                .map(ReceiptDetailItemDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (detailSum.compareTo(dto.getTotalAmount()) != 0) {
            throw new FinBizException(FinErrorCode.FIN_4008,
                    "明细合计=" + detailSum + "，总金额=" + dto.getTotalAmount());
        }
    }

    /** DTO → Entity 字段填充 */
    private void fillFromDTO(FinReceipt receipt, ReceiptCreateDTO dto) {
        receipt.setContractId(dto.getContractId());
        receipt.setBrandId(dto.getBrandId());
        receipt.setShopCode(dto.getShopCode());
        receipt.setTotalAmount(dto.getTotalAmount());
        receipt.setPaymentMethod(dto.getPaymentMethod());
        receipt.setBankSerialNo(dto.getBankSerialNo());
        receipt.setPayerName(dto.getPayerName());
        receipt.setBankName(dto.getBankName());
        receipt.setBankAccount(dto.getBankAccount());
        receipt.setIsUnnamed(dto.getIsUnnamed());
        receipt.setAccountingEntity(dto.getAccountingEntity());
        receipt.setReceiptDate(dto.getReceiptDate());
        receipt.setReceiver(dto.getReceiver());
    }

    /** 从合同带出项目ID和商家ID */
    private void fillContractInfo(FinReceipt receipt, Long contractId) {
        try {
            Map<String, Object> c = jdbcTemplate.queryForMap(
                    "SELECT project_id, merchant_id FROM inv_lease_contract WHERE id=? LIMIT 1", contractId);
            receipt.setProjectId(toLong(c.get("project_id")));
            receipt.setMerchantId(toLong(c.get("merchant_id")));
        } catch (Exception e) {
            log.warn("[收款单] 未查到合同信息 contractId={}", contractId);
        }
    }

    /** 保存拆分明细（无明细时自动创建全额明细） */
    private void saveDetails(Long receiptId, BigDecimal totalAmount, List<ReceiptDetailItemDTO> items) {
        List<FinReceiptDetail> details = new ArrayList<>();
        if (CollectionUtils.isEmpty(items)) {
            // 无明细：自动创建一条全额明细
            FinReceiptDetail d = new FinReceiptDetail();
            d.setReceiptId(receiptId);
            d.setAmount(totalAmount);
            d.setFeeName("收款");
            details.add(d);
        } else {
            for (ReceiptDetailItemDTO item : items) {
                FinReceiptDetail d = new FinReceiptDetail();
                d.setReceiptId(receiptId);
                d.setFeeItemId(item.getFeeItemId());
                d.setFeeName(item.getFeeName());
                d.setAmount(item.getAmount());
                d.setRemark(item.getRemark());
                details.add(d);
            }
        }
        for (FinReceiptDetail d : details) {
            detailMapper.insert(d);
        }
    }

    /** Entity → VO 转换（不含明细列表，详情页单独查） */
    private ReceiptDetailVO toDetailVO(FinReceipt r) {
        ReceiptDetailVO vo = new ReceiptDetailVO();
        vo.setId(r.getId());
        vo.setReceiptCode(r.getReceiptCode());
        vo.setContractId(r.getContractId());
        vo.setProjectId(r.getProjectId());
        vo.setMerchantId(r.getMerchantId());
        vo.setBrandId(r.getBrandId());
        vo.setShopCode(r.getShopCode());
        vo.setTotalAmount(r.getTotalAmount());
        vo.setPaymentMethod(r.getPaymentMethod());
        vo.setPaymentMethodName(paymentMethodName(r.getPaymentMethod()));
        vo.setBankSerialNo(r.getBankSerialNo());
        vo.setPayerName(r.getPayerName());
        vo.setBankName(r.getBankName());
        vo.setBankAccount(r.getBankAccount());
        vo.setIsUnnamed(r.getIsUnnamed());
        vo.setAccountingEntity(r.getAccountingEntity());
        vo.setReceiptDate(r.getReceiptDate());
        vo.setReceiver(r.getReceiver());
        vo.setStatus(r.getStatus());
        vo.setStatusName(statusName(r.getStatus()));
        vo.setWriteOffAmount(r.getWriteOffAmount() != null ? r.getWriteOffAmount() : BigDecimal.ZERO);
        vo.setPrepayAmount(r.getPrepayAmount() != null ? r.getPrepayAmount() : BigDecimal.ZERO);

        // 冗余展示字段：合同/项目/商家名称
        if (r.getContractId() != null) {
            try {
                Map<String, Object> c = jdbcTemplate.queryForMap(
                        "SELECT contract_code, contract_name FROM inv_lease_contract WHERE id=? LIMIT 1",
                        r.getContractId());
                vo.setContractCode(str(c.get("contract_code")));
                vo.setContractName(str(c.get("contract_name")));
            } catch (Exception ignored) {}
        }
        if (r.getProjectId() != null) {
            try { vo.setProjectName(jdbcTemplate.queryForObject(
                    "SELECT project_name FROM biz_project WHERE id=? LIMIT 1", String.class, r.getProjectId()));
            } catch (Exception ignored) {}
        }
        if (r.getMerchantId() != null) {
            try { vo.setMerchantName(jdbcTemplate.queryForObject(
                    "SELECT merchant_name FROM biz_merchant WHERE id=? LIMIT 1", String.class, r.getMerchantId()));
            } catch (Exception ignored) {}
        }
        return vo;
    }

    private String statusName(Integer s) {
        if (s == null) return "";
        return switch (s) {
            case 0 -> "待核销"; case 1 -> "部分核销"; case 2 -> "已全部核销"; case 3 -> "已作废"; default -> "";
        };
    }

    private String paymentMethodName(Integer m) {
        if (m == null) return "";
        return switch (m) {
            case 1 -> "银行转账"; case 2 -> "现金"; case 3 -> "支票"; case 4 -> "POS"; default -> "";
        };
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private String str(Object v) { return v != null ? v.toString() : null; }
}
