package com.asset.finance.receipt.service.impl;

import com.asset.finance.common.adapter.OaApprovalAdapter;
import com.asset.finance.common.exception.FinBizException;
import com.asset.finance.common.exception.FinErrorCode;
import com.asset.finance.common.util.FinCodeGenerator;
import com.asset.finance.prepayment.entity.FinPrepayAccount;
import com.asset.finance.prepayment.entity.FinPrepayTransaction;
import com.asset.finance.prepayment.mapper.FinPrepayAccountMapper;
import com.asset.finance.prepayment.mapper.FinPrepayTransactionMapper;
import com.asset.finance.receipt.dto.*;
import com.asset.finance.receipt.entity.FinReceipt;
import com.asset.finance.receipt.entity.FinWriteOff;
import com.asset.finance.receipt.entity.FinWriteOffDetail;
import com.asset.finance.receipt.mapper.FinReceiptMapper;
import com.asset.finance.receipt.mapper.FinWriteOffDetailMapper;
import com.asset.finance.receipt.mapper.FinWriteOffMapper;
import com.asset.finance.receipt.service.FinWriteOffService;
import com.asset.finance.receivable.entity.FinReceivable;
import com.asset.finance.receivable.mapper.FinReceivableMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.util.List;
import java.util.Map;

/**
 * 核销管理 ServiceImpl
 *
 * <p>核销流程：
 * <ol>
 *   <li>submitWriteOff：校验收款余额 → 创建核销单（status=0）→ 提交OA审批</li>
 *   <li>approveCallback（@Transactional）：审批通过时逐行更新应收已收/欠费 → 超额转预存款 → 更新收款单余额和状态</li>
 *   <li>cancelWriteOff：仅 status=0 可撤销</li>
 * </ol>
 *
 * <p>乐观锁保护：应收/收款单均有 @Version 字段，更新失败时抛 FIN_5001。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinWriteOffServiceImpl extends ServiceImpl<FinWriteOffMapper, FinWriteOff>
        implements FinWriteOffService {

    private final FinReceivableMapper receivableMapper;
    private final FinReceiptMapper receiptMapper;
    private final FinWriteOffDetailMapper writeOffDetailMapper;
    private final FinPrepayAccountMapper prepayAccountMapper;
    private final FinPrepayTransactionMapper prepayTransactionMapper;
    private final FinCodeGenerator codeGenerator;
    private final OaApprovalAdapter oaApprovalAdapter;
    private final JdbcTemplate jdbcTemplate;

    private static final String[] WRITE_OFF_TYPE_NAMES = {null, "收款核销", "保证金核销", "预收款核销", "负数核销"};
    private static final String[] STATUS_NAMES = {"待审核", "审核通过", "已撤销"};

    // ─── 分页查询 ─────────────────────────────────────────────────────────────
    @Override
    public IPage<WriteOffDetailVO> pageQuery(WriteOffQueryDTO query) {
        LambdaQueryWrapper<FinWriteOff> wrapper = new LambdaQueryWrapper<FinWriteOff>()
                .eq(query.getContractId() != null, FinWriteOff::getContractId, query.getContractId())
                .eq(query.getMerchantId() != null, FinWriteOff::getMerchantId, query.getMerchantId())
                .eq(query.getProjectId() != null, FinWriteOff::getProjectId, query.getProjectId())
                .eq(query.getReceiptId() != null, FinWriteOff::getReceiptId, query.getReceiptId())
                .eq(query.getStatus() != null, FinWriteOff::getStatus, query.getStatus())
                .eq(query.getWriteOffType() != null, FinWriteOff::getWriteOffType, query.getWriteOffType())
                .like(query.getWriteOffCode() != null, FinWriteOff::getWriteOffCode, query.getWriteOffCode())
                .orderByDesc(FinWriteOff::getId);

        IPage<FinWriteOff> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return page.convert(wo -> toDetailVO(wo, false));
    }

    // ─── 可核销应收列表 ────────────────────────────────────────────────────────
    @Override
    public List<WritableReceivableVO> queryWritableReceivables(Long contractId) {
        List<FinReceivable> list = receivableMapper.selectList(
                new LambdaQueryWrapper<FinReceivable>()
                        .eq(FinReceivable::getContractId, contractId)
                        .in(FinReceivable::getStatus, 0, 1)
                        .orderByAsc(FinReceivable::getDueDate)
        );
        return list.stream().map(r -> {
            WritableReceivableVO vo = new WritableReceivableVO();
            vo.setId(r.getId());
            vo.setReceivableCode(r.getReceivableCode());
            vo.setFeeItemId(r.getFeeItemId());
            vo.setFeeName(r.getFeeName());
            vo.setAccrualMonth(r.getAccrualMonth());
            vo.setBillingStart(r.getBillingStart());
            vo.setBillingEnd(r.getBillingEnd());
            vo.setDueDate(r.getDueDate());
            vo.setActualAmount(r.getActualAmount());
            vo.setReceivedAmount(r.getReceivedAmount());
            vo.setOutstandingAmount(r.getOutstandingAmount());
            vo.setStatus(r.getStatus());
            return vo;
        }).toList();
    }

    // ─── 提交核销申请 ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitWriteOff(Long receiptId, List<WriteOffDetailItemDTO> items, Integer writeOffType) {
        // 1. 加锁查询收款单
        FinReceipt receipt = receiptMapper.selectByIdForUpdate(receiptId);
        if (receipt == null) {
            throw new FinBizException(FinErrorCode.FIN_4009, "收款单不存在");
        }

        // 2. 计算本次核销总额
        BigDecimal totalWriteOff = items.stream()
                .map(WriteOffDetailItemDTO::getWriteOffAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int type = writeOffType != null ? writeOffType : 1;

        // 3. 校验负数核销：金额须为负
        if (type == 4) {
            if (totalWriteOff.compareTo(BigDecimal.ZERO) >= 0) {
                throw new FinBizException(FinErrorCode.FIN_4005);
            }
        } else {
            // 普通核销：余额校验
            BigDecimal alreadyWritten = receipt.getWriteOffAmount() == null ? BigDecimal.ZERO : receipt.getWriteOffAmount();
            BigDecimal remaining = receipt.getTotalAmount().subtract(alreadyWritten);
            if (totalWriteOff.compareTo(remaining) > 0) {
                throw new FinBizException(FinErrorCode.FIN_4001,
                        String.format("本次核销 %.2f，收款余额 %.2f", totalWriteOff, remaining));
            }
        }

        // 4. 创建核销单（status=0 待审核）
        FinWriteOff writeOff = new FinWriteOff();
        writeOff.setWriteOffCode(codeGenerator.writeOffCode());
        writeOff.setReceiptId(receiptId);
        writeOff.setContractId(receipt.getContractId());
        writeOff.setMerchantId(receipt.getMerchantId());
        writeOff.setProjectId(receipt.getProjectId());
        writeOff.setWriteOffType(type);
        writeOff.setTotalAmount(totalWriteOff);
        writeOff.setStatus(0);
        save(writeOff);

        // 5. 保存核销明细行
        for (WriteOffDetailItemDTO item : items) {
            FinReceivable receivable = receivableMapper.selectById(item.getReceivableId());
            FinWriteOffDetail detail = new FinWriteOffDetail();
            detail.setWriteOffId(writeOff.getId());
            detail.setReceivableId(item.getReceivableId());
            detail.setFeeItemId(receivable != null ? receivable.getFeeItemId() : item.getFeeItemId());
            detail.setAccrualMonth(receivable != null ? receivable.getAccrualMonth() : item.getAccrualMonth());
            detail.setWriteOffAmount(item.getWriteOffAmount());
            detail.setOverpayAmount(BigDecimal.ZERO);
            writeOffDetailMapper.insert(detail);
        }

        // 6. 提交 OA 审批（非关键步骤，失败不回滚）
        try {
            String approvalId = oaApprovalAdapter.submitApproval(
                    "FIN_WRITE_OFF",
                    writeOff.getId(),
                    "核销单审批-" + writeOff.getWriteOffCode()
            );
            writeOff.setApprovalId(approvalId);
            updateById(writeOff);
        } catch (Exception e) {
            log.warn("[核销] OA提交失败，核销单 {} 将手动推进审批", writeOff.getWriteOffCode(), e);
        }

        return writeOff.getId();
    }

    // ─── 审批回调（核心事务）────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveCallback(String approvalId, boolean approved, String comment) {
        // 1. 找到核销单
        FinWriteOff writeOff = getOne(
                new LambdaQueryWrapper<FinWriteOff>().eq(FinWriteOff::getApprovalId, approvalId)
        );
        if (writeOff == null) {
            throw new FinBizException(FinErrorCode.FIN_4009, "找不到对应核销单，approvalId=" + approvalId);
        }
        // 幂等保护：已处理过不重复
        if (writeOff.getStatus() != 0) {
            log.warn("[核销回调] 核销单 {} 已处于 status={}，忽略重复回调", writeOff.getWriteOffCode(), writeOff.getStatus());
            return;
        }

        if (!approved) {
            writeOff.setStatus(2); // 驳回/撤销
            updateById(writeOff);
            return;
        }

        // 2. 加锁查询收款单
        FinReceipt receipt = receiptMapper.selectByIdForUpdate(writeOff.getReceiptId());
        if (receipt == null) {
            throw new FinBizException(FinErrorCode.FIN_4009, "收款单不存在");
        }

        // 3. 逐行处理核销明细
        List<FinWriteOffDetail> details = writeOffDetailMapper.selectList(
                new LambdaQueryWrapper<FinWriteOffDetail>().eq(FinWriteOffDetail::getWriteOffId, writeOff.getId())
        );

        BigDecimal totalOverpay = BigDecimal.ZERO;

        for (FinWriteOffDetail detail : details) {
            FinReceivable receivable = receivableMapper.selectByIdForUpdate(detail.getReceivableId());
            if (receivable == null) {
                log.warn("[核销回调] 应收记录 {} 不存在，跳过", detail.getReceivableId());
                continue;
            }

            BigDecimal writeOffAmt = detail.getWriteOffAmount();
            BigDecimal outstanding = receivable.getOutstandingAmount();
            BigDecimal actualWriteOff;
            BigDecimal overpay = BigDecimal.ZERO;

            if (writeOffAmt.compareTo(outstanding) > 0) {
                // 超额：多余转预存款
                actualWriteOff = outstanding;
                overpay = writeOffAmt.subtract(outstanding);
                detail.setOverpayAmount(overpay);
                writeOffDetailMapper.updateById(detail);
                totalOverpay = totalOverpay.add(overpay);
            } else {
                actualWriteOff = writeOffAmt;
            }

            // 更新应收台账
            BigDecimal newReceived = receivable.getReceivedAmount().add(actualWriteOff);
            BigDecimal newOutstanding = receivable.getActualAmount().subtract(newReceived);
            receivable.setReceivedAmount(newReceived);
            receivable.setOutstandingAmount(newOutstanding.max(BigDecimal.ZERO));
            receivable.setStatus(newOutstanding.compareTo(BigDecimal.ZERO) <= 0 ? 2 : 1);

            int updated = receivableMapper.updateById(receivable);
            if (updated == 0) {
                throw new FinBizException(FinErrorCode.FIN_5001, "应收记录 " + receivable.getId() + " 乐观锁冲突");
            }

            // 超额转预存款
            if (overpay.compareTo(BigDecimal.ZERO) > 0) {
                transferToPrepaAccount(writeOff, overpay);
            }
        }

        // 4. 更新收款单已核销金额、转存金额、状态
        BigDecimal prevWriteOff = receipt.getWriteOffAmount() == null ? BigDecimal.ZERO : receipt.getWriteOffAmount();
        BigDecimal newWriteOff = prevWriteOff.add(writeOff.getTotalAmount());
        receipt.setWriteOffAmount(newWriteOff);

        if (totalOverpay.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal prevPrepay = receipt.getPrepayAmount() == null ? BigDecimal.ZERO : receipt.getPrepayAmount();
            receipt.setPrepayAmount(prevPrepay.add(totalOverpay));
        }

        BigDecimal remaining = receipt.getTotalAmount().subtract(newWriteOff);
        receipt.setStatus(remaining.compareTo(BigDecimal.ZERO) <= 0 ? 2 : 1);

        int receiptUpdated = receiptMapper.updateById(receipt);
        if (receiptUpdated == 0) {
            throw new FinBizException(FinErrorCode.FIN_5001, "收款单乐观锁冲突");
        }

        // 5. 核销单状态改为已通过
        writeOff.setStatus(1);
        updateById(writeOff);

        log.info("[核销回调] 核销单 {} 审批通过", writeOff.getWriteOffCode());
    }

    /**
     * 超额部分转入预收款账户（通用账户）
     */
    private void transferToPrepaAccount(FinWriteOff writeOff, BigDecimal overpay) {
        FinPrepayAccount account = prepayAccountMapper.selectByContractId(writeOff.getContractId());
        if (account == null) {
            account = new FinPrepayAccount();
            account.setContractId(writeOff.getContractId());
            account.setMerchantId(writeOff.getMerchantId());
            account.setProjectId(writeOff.getProjectId());
            account.setBalance(BigDecimal.ZERO);
            prepayAccountMapper.insert(account);
        } else {
            account = prepayAccountMapper.selectByIdForUpdate(account.getId());
        }

        BigDecimal newBalance = account.getBalance().add(overpay);
        account.setBalance(newBalance);
        prepayAccountMapper.updateById(account);

        FinPrepayTransaction tx = new FinPrepayTransaction();
        tx.setAccountId(account.getId());
        tx.setTransType(1); // 转入
        tx.setAmount(overpay);
        tx.setBalanceAfter(newBalance);
        tx.setTransDate(LocalDate.now());
        tx.setSourceCode(writeOff.getWriteOffCode());
        tx.setRemark("核销超额转入预存款");
        prepayTransactionMapper.insert(tx);

        log.info("[核销] 超额 {} 元转入预收款账户（contractId={}）", overpay, writeOff.getContractId());
    }

    // ─── 撤销核销单 ────────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelWriteOff(Long id) {
        FinWriteOff writeOff = getById(id);
        if (writeOff == null || writeOff.getStatus() != 0) {
            throw new FinBizException(FinErrorCode.FIN_4003, "仅待审核状态的核销单可撤销");
        }
        writeOff.setStatus(2);
        updateById(writeOff);
        log.info("[核销] 核销单 {} 已撤销", writeOff.getWriteOffCode());
    }

    // ─── 查看详情 ──────────────────────────────────────────────────────────────
    @Override
    public WriteOffDetailVO getDetailById(Long id) {
        FinWriteOff writeOff = getById(id);
        if (writeOff == null) {
            return null;
        }
        WriteOffDetailVO vo = toDetailVO(writeOff, true);
        vo.setDetails(writeOffDetailMapper.selectList(
                new LambdaQueryWrapper<FinWriteOffDetail>().eq(FinWriteOffDetail::getWriteOffId, id)
        ));
        return vo;
    }

    // ─── 转换 VO ──────────────────────────────────────────────────────────────
    private WriteOffDetailVO toDetailVO(FinWriteOff wo, boolean fetchNames) {
        WriteOffDetailVO vo = new WriteOffDetailVO();
        vo.setId(wo.getId());
        vo.setWriteOffCode(wo.getWriteOffCode());
        vo.setReceiptId(wo.getReceiptId());
        vo.setContractId(wo.getContractId());
        vo.setMerchantId(wo.getMerchantId());
        vo.setProjectId(wo.getProjectId());
        vo.setWriteOffType(wo.getWriteOffType());
        if (wo.getWriteOffType() != null && wo.getWriteOffType() >= 1 && wo.getWriteOffType() <= 4) {
            vo.setWriteOffTypeName(WRITE_OFF_TYPE_NAMES[wo.getWriteOffType()]);
        }
        vo.setTotalAmount(wo.getTotalAmount());
        vo.setStatus(wo.getStatus());
        if (wo.getStatus() != null && wo.getStatus() >= 0 && wo.getStatus() < STATUS_NAMES.length) {
            vo.setStatusName(STATUS_NAMES[wo.getStatus()]);
        }
        vo.setApprovalId(wo.getApprovalId());
        vo.setCreateTime(wo.getCreatedAt());
        vo.setUpdateTime(wo.getUpdatedAt());

        if (fetchNames) {
            try {
                if (wo.getReceiptId() != null) {
                    String receiptCode = jdbcTemplate.queryForObject(
                            "SELECT receipt_code FROM fin_receipt WHERE id=? AND is_deleted=0",
                            String.class, wo.getReceiptId());
                    vo.setReceiptCode(receiptCode);
                }
                if (wo.getContractId() != null) {
                    Map<String, Object> contract = jdbcTemplate.queryForMap(
                            "SELECT contract_code, contract_name FROM inv_lease_contract WHERE id=? AND is_deleted=0",
                            wo.getContractId());
                    vo.setContractCode((String) contract.get("contract_code"));
                    vo.setContractName((String) contract.get("contract_name"));
                }
                if (wo.getMerchantId() != null) {
                    String merchantName = jdbcTemplate.queryForObject(
                            "SELECT merchant_name FROM biz_merchant WHERE id=? AND is_deleted=0",
                            String.class, wo.getMerchantId());
                    vo.setMerchantName(merchantName);
                }
                if (wo.getProjectId() != null) {
                    String projectName = jdbcTemplate.queryForObject(
                            "SELECT project_name FROM biz_project WHERE id=? AND is_deleted=0",
                            String.class, wo.getProjectId());
                    vo.setProjectName(projectName);
                }
            } catch (Exception e) {
                log.debug("[核销VO] 补充关联名称异常：{}", e.getMessage());
            }
        }

        return vo;
    }
}
