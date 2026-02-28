package com.asset.finance.prepayment.service.impl;

import com.asset.common.exception.BizException;
import com.asset.finance.common.exception.FinBizException;
import com.asset.finance.common.exception.FinErrorCode;
import com.asset.finance.prepayment.dto.*;
import com.asset.finance.prepayment.entity.FinPrepayAccount;
import com.asset.finance.prepayment.entity.FinPrepayTransaction;
import com.asset.finance.prepayment.mapper.FinPrepayAccountMapper;
import com.asset.finance.prepayment.mapper.FinPrepayTransactionMapper;
import com.asset.finance.prepayment.service.FinPrepaymentService;
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
import java.util.Map;

/**
 * 预收款管理 ServiceImpl
 *
 * <p>余额恒等式：balance = Σ转入 - Σ抵冲 - Σ退款
 * <p>所有操作直接生效，无需 OA 审批。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinPrepaymentServiceImpl extends ServiceImpl<FinPrepayAccountMapper, FinPrepayAccount>
        implements FinPrepaymentService {

    private final FinPrepayTransactionMapper transactionMapper;
    private final FinReceivableMapper receivableMapper;
    private final JdbcTemplate jdbcTemplate;

    // 交易类型常量
    private static final int TRANS_IN     = 1; // 转入
    private static final int TRANS_OFFSET = 2; // 抵冲
    private static final int TRANS_REFUND = 3; // 退款

    // ─── 查询账户 ─────────────────────────────────────────────────────────────
    @Override
    public PrepayAccountVO getAccount(Long contractId) {
        FinPrepayAccount account = baseMapper.selectByContractId(contractId);
        if (account == null) return null;
        return toAccountVO(account);
    }

    // ─── 分页查询流水 ─────────────────────────────────────────────────────────
    @Override
    public IPage<FinPrepayTransaction> pageTransaction(PrepayQueryDTO query) {
        LambdaQueryWrapper<FinPrepayTransaction> wrapper = new LambdaQueryWrapper<FinPrepayTransaction>()
                .eq(query.getAccountId() != null, FinPrepayTransaction::getAccountId, query.getAccountId())
                .eq(query.getTransType() != null, FinPrepayTransaction::getTransType, query.getTransType())
                .orderByDesc(FinPrepayTransaction::getId);

        if (query.getContractId() != null && query.getAccountId() == null) {
            FinPrepayAccount account = baseMapper.selectByContractId(query.getContractId());
            if (account == null) return new Page<>(query.getPageNum(), query.getPageSize());
            wrapper.eq(FinPrepayTransaction::getAccountId, account.getId());
        }

        return transactionMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    // ─── 手动录入预收款 ───────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deposit(PrepayDepositDTO dto) {
        addBalance(dto.getContractId(), null, dto.getAmount(), dto.getSourceCode(), dto.getRemark());
    }

    // ─── 增加余额（内部调用入口）─────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBalance(Long contractId, Long accountId, BigDecimal amount, String sourceCode, String remark) {
        FinPrepayAccount account;
        if (accountId != null) {
            account = baseMapper.selectByIdForUpdate(accountId);
            if (account == null) throw new BizException("预收款账户不存在: id=" + accountId);
        } else {
            account = baseMapper.selectByContractIdForUpdate(contractId);
            if (account == null) {
                account = createAccount(contractId);
            }
        }

        BigDecimal newBalance = safe(account.getBalance()).add(amount);
        account.setBalance(newBalance);
        int updated = baseMapper.updateById(account);
        if (updated == 0) throw new FinBizException(FinErrorCode.FIN_5001, "预收款账户乐观锁冲突");

        insertTx(account, TRANS_IN, amount, newBalance, sourceCode, remark);
        log.info("[预收款] 转入 {} 元，合同 {}，余额→{}", amount, account.getContractId(), newBalance);
    }

    // ─── 抵冲应收 ─────────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offset(PrepayOffsetDTO dto) {
        FinPrepayAccount account = getAccountOrThrow(dto.getContractId());
        checkBalance(account, dto.getAmount(), "抵冲");

        // 校验应收记录
        FinReceivable receivable = receivableMapper.selectByIdForUpdate(dto.getReceivableId());
        if (receivable == null || !dto.getContractId().equals(receivable.getContractId())) {
            throw new BizException("应收记录不存在或不属于该合同");
        }
        if (receivable.getStatus() != null && receivable.getStatus() >= 2) {
            throw new BizException("该应收记录已收清，无需抵冲");
        }
        if (dto.getAmount().compareTo(receivable.getOutstandingAmount()) > 0) {
            throw new FinBizException(FinErrorCode.FIN_4002,
                    String.format("抵冲金额 %.2f 超过应收未收金额 %.2f", dto.getAmount(), receivable.getOutstandingAmount()));
        }

        // 扣减预收余额（加行锁）
        FinPrepayAccount locked = baseMapper.selectByIdForUpdate(account.getId());
        checkBalance(locked, dto.getAmount(), "抵冲");

        BigDecimal newBalance = locked.getBalance().subtract(dto.getAmount());
        locked.setBalance(newBalance);
        int updated = baseMapper.updateById(locked);
        if (updated == 0) throw new FinBizException(FinErrorCode.FIN_5001, "预收款账户乐观锁冲突");

        // 更新应收台账
        BigDecimal newReceived = receivable.getReceivedAmount().add(dto.getAmount());
        BigDecimal newOutstanding = receivable.getActualAmount().subtract(newReceived).max(BigDecimal.ZERO);
        receivable.setReceivedAmount(newReceived);
        receivable.setOutstandingAmount(newOutstanding);
        receivable.setStatus(newOutstanding.compareTo(BigDecimal.ZERO) <= 0 ? 2 : 1);
        receivableMapper.updateById(receivable);

        insertTx(locked, TRANS_OFFSET, dto.getAmount(), newBalance,
                "REC:" + dto.getReceivableId(), dto.getRemark());

        log.info("[预收款] 抵冲应收 {} 金额 {} 元，余额→{}", dto.getReceivableId(), dto.getAmount(), newBalance);
    }

    // ─── 退款 ────────────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(PrepayRefundDTO dto) {
        FinPrepayAccount account = getAccountOrThrow(dto.getContractId());
        checkBalance(account, dto.getAmount(), "退款");

        // 加行锁
        FinPrepayAccount locked = baseMapper.selectByIdForUpdate(account.getId());
        checkBalance(locked, dto.getAmount(), "退款");

        BigDecimal newBalance = locked.getBalance().subtract(dto.getAmount());
        locked.setBalance(newBalance);
        int updated = baseMapper.updateById(locked);
        if (updated == 0) throw new FinBizException(FinErrorCode.FIN_5001, "预收款账户乐观锁冲突");

        String remark = buildRefundRemark(dto);
        insertTx(locked, TRANS_REFUND, dto.getAmount(), newBalance, null, remark);

        log.info("[预收款] 退款 {} 元，合同 {}，余额→{}", dto.getAmount(), dto.getContractId(), newBalance);
    }

    // ─── 私有辅助 ─────────────────────────────────────────────────────────────

    private FinPrepayAccount getAccountOrThrow(Long contractId) {
        FinPrepayAccount account = baseMapper.selectByContractId(contractId);
        if (account == null) throw new BizException("合同 " + contractId + " 尚无预收款账户");
        return account;
    }

    private void checkBalance(FinPrepayAccount account, BigDecimal required, String opName) {
        BigDecimal balance = safe(account.getBalance());
        if (balance.compareTo(required) < 0) {
            throw new FinBizException(FinErrorCode.FIN_4002,
                    String.format("%s金额 %.2f 超过可用余额 %.2f", opName, required, balance));
        }
    }

    private FinPrepayAccount createAccount(Long contractId) {
        FinPrepayAccount account = new FinPrepayAccount();
        account.setContractId(contractId);
        account.setBalance(BigDecimal.ZERO);
        try {
            Map<String, Object> contract = jdbcTemplate.queryForMap(
                    "SELECT project_id, merchant_id FROM inv_lease_contract WHERE id=? AND is_deleted=0 LIMIT 1",
                    contractId);
            account.setProjectId(toLong(contract.get("project_id")));
            account.setMerchantId(toLong(contract.get("merchant_id")));
        } catch (Exception ignored) {}
        baseMapper.insert(account);
        return account;
    }

    private void insertTx(FinPrepayAccount account, int transType, BigDecimal amount,
                          BigDecimal balanceAfter, String sourceCode, String remark) {
        FinPrepayTransaction tx = new FinPrepayTransaction();
        tx.setAccountId(account.getId());
        tx.setTransType(transType);
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setTransDate(LocalDate.now());
        tx.setSourceCode(sourceCode);
        tx.setRemark(remark);
        transactionMapper.insert(tx);
    }

    private PrepayAccountVO toAccountVO(FinPrepayAccount account) {
        PrepayAccountVO vo = new PrepayAccountVO();
        vo.setId(account.getId());
        vo.setContractId(account.getContractId());
        vo.setMerchantId(account.getMerchantId());
        vo.setProjectId(account.getProjectId());
        vo.setFeeItemId(account.getFeeItemId());
        vo.setBalance(safe(account.getBalance()));
        vo.setCreateTime(account.getCreatedAt());
        vo.setUpdateTime(account.getUpdatedAt());
        try {
            if (account.getContractId() != null) {
                Map<String, Object> c = jdbcTemplate.queryForMap(
                        "SELECT contract_code, contract_name FROM inv_lease_contract WHERE id=? AND is_deleted=0 LIMIT 1",
                        account.getContractId());
                vo.setContractCode((String) c.get("contract_code"));
                vo.setContractName((String) c.get("contract_name"));
            }
            if (account.getMerchantId() != null) {
                vo.setMerchantName(jdbcTemplate.queryForObject(
                        "SELECT merchant_name FROM biz_merchant WHERE id=? AND is_deleted=0 LIMIT 1",
                        String.class, account.getMerchantId()));
            }
            if (account.getProjectId() != null) {
                vo.setProjectName(jdbcTemplate.queryForObject(
                        "SELECT project_name FROM biz_project WHERE id=? AND is_deleted=0 LIMIT 1",
                        String.class, account.getProjectId()));
            }
        } catch (Exception e) {
            log.debug("[预收款VO] 补充名称异常：{}", e.getMessage());
        }
        return vo;
    }

    private String buildRefundRemark(PrepayRefundDTO dto) {
        StringBuilder sb = new StringBuilder("退款");
        if (dto.getPayee() != null) sb.append(" 收款人：").append(dto.getPayee());
        if (dto.getBankAccount() != null) sb.append(" 账号：").append(dto.getBankAccount());
        if (dto.getRemark() != null) sb.append(" 备注：").append(dto.getRemark());
        return sb.toString();
    }

    private BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}
