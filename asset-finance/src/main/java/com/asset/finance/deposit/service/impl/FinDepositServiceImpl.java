package com.asset.finance.deposit.service.impl;

import com.asset.common.exception.BizException;
import com.asset.finance.common.adapter.OaApprovalAdapter;
import com.asset.finance.common.exception.FinBizException;
import com.asset.finance.common.exception.FinErrorCode;
import com.asset.finance.deposit.dto.*;
import com.asset.finance.deposit.entity.FinDepositAccount;
import com.asset.finance.deposit.entity.FinDepositTransaction;
import com.asset.finance.deposit.mapper.FinDepositAccountMapper;
import com.asset.finance.deposit.mapper.FinDepositTransactionMapper;
import com.asset.finance.deposit.service.FinDepositService;
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
 * 保证金管理 ServiceImpl
 *
 * <p>余额恒等式：balance = total_in - total_offset - total_refund - total_forfeit
 *
 * <p>操作流程（offset/refund/forfeit）：
 * <ol>
 *   <li>余额预校验（不加锁，快速失败）</li>
 *   <li>创建待审核流水（status=0）</li>
 *   <li>提交 OA 审批</li>
 *   <li>approveCallback @Transactional：加锁账户 → 再次校验余额 → 扣减余额 → 更新流水 status=1</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinDepositServiceImpl extends ServiceImpl<FinDepositAccountMapper, FinDepositAccount>
        implements FinDepositService {

    private final FinDepositTransactionMapper transactionMapper;
    private final FinReceivableMapper receivableMapper;
    private final OaApprovalAdapter oaApprovalAdapter;
    private final JdbcTemplate jdbcTemplate;

    // 交易类型常量
    private static final int TRANS_PAY_IN  = 1;
    private static final int TRANS_OFFSET  = 2;
    private static final int TRANS_REFUND  = 3;
    private static final int TRANS_FORFEIT = 4;

    // ─── 查询账户 ─────────────────────────────────────────────────────────────
    @Override
    public DepositAccountVO getAccount(Long contractId) {
        FinDepositAccount account = baseMapper.selectByContractId(contractId);
        if (account == null) return null;
        return toAccountVO(account);
    }

    // ─── 分页查询流水 ─────────────────────────────────────────────────────────
    @Override
    public IPage<FinDepositTransaction> pageTransaction(DepositQueryDTO query) {
        LambdaQueryWrapper<FinDepositTransaction> wrapper = new LambdaQueryWrapper<FinDepositTransaction>()
                .eq(query.getAccountId() != null, FinDepositTransaction::getAccountId, query.getAccountId())
                .eq(query.getTransType() != null, FinDepositTransaction::getTransType, query.getTransType())
                .eq(query.getStatus() != null, FinDepositTransaction::getStatus, query.getStatus())
                .orderByDesc(FinDepositTransaction::getId);

        // 如果按 contractId 查，先找 accountId
        if (query.getContractId() != null && query.getAccountId() == null) {
            FinDepositAccount account = baseMapper.selectByContractId(query.getContractId());
            if (account == null) return new Page<>(query.getPageNum(), query.getPageSize());
            wrapper.eq(FinDepositTransaction::getAccountId, account.getId());
        }

        return transactionMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    // ─── 缴纳保证金（直接生效）────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payIn(DepositPayInDTO dto) {
        // 查找或创建账户
        FinDepositAccount account = baseMapper.selectByContractIdForUpdate(dto.getContractId());
        if (account == null) {
            account = createAccount(dto.getContractId());
        }

        BigDecimal newBalance = account.getBalance().add(dto.getAmount());
        BigDecimal newTotalIn = (account.getTotalIn() == null ? BigDecimal.ZERO : account.getTotalIn()).add(dto.getAmount());
        account.setBalance(newBalance);
        account.setTotalIn(newTotalIn);
        baseMapper.updateById(account);

        // 记录流水（直接生效，status=1）
        FinDepositTransaction tx = buildTx(account, TRANS_PAY_IN, dto.getAmount(), newBalance,
                dto.getSourceCode(), dto.getReason(), 1);
        transactionMapper.insert(tx);

        log.info("[保证金] 缴纳 {} 元，合同 {}，余额→{}", dto.getAmount(), dto.getContractId(), newBalance);
    }

    // ─── 申请冲抵 ─────────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long processOffset(DepositOffsetDTO dto) {
        FinDepositAccount account = getAccountOrThrow(dto.getContractId());
        checkBalance(account, dto.getAmount(), "冲抵");

        // 验证应收记录存在且属于该合同
        FinReceivable receivable = receivableMapper.selectById(dto.getReceivableId());
        if (receivable == null || !dto.getContractId().equals(receivable.getContractId())) {
            throw new BizException("应收记录不存在或不属于该合同");
        }
        if (receivable.getStatus() == null || receivable.getStatus() >= 2) {
            throw new BizException("该应收记录已收清，无需冲抵");
        }

        String code = "OFFSET-" + dto.getContractId() + "-" + System.currentTimeMillis();
        FinDepositTransaction tx = buildTx(account, TRANS_OFFSET, dto.getAmount(), null,
                code, dto.getReason() != null ? dto.getReason() : "保证金冲抵应收", 0);
        // 存储 receivableId 到 sourceCode 以便回调时使用
        tx.setSourceCode("REC:" + dto.getReceivableId() + "|" + code);
        transactionMapper.insert(tx);

        submitApproval(tx, "FIN_DEPOSIT_OP", "保证金冲抵-" + code);
        return tx.getId();
    }

    // ─── 申请退款 ─────────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long processRefund(DepositRefundDTO dto) {
        FinDepositAccount account = getAccountOrThrow(dto.getContractId());
        checkBalance(account, dto.getAmount(), "退款");

        String code = "REFUND-" + dto.getContractId() + "-" + System.currentTimeMillis();
        String remark = "退款" + (dto.getReason() != null ? "：" + dto.getReason() : "");
        FinDepositTransaction tx = buildTx(account, TRANS_REFUND, dto.getAmount(), null, code, remark, 0);
        transactionMapper.insert(tx);

        submitApproval(tx, "FIN_DEPOSIT_OP", "保证金退款-" + code);
        return tx.getId();
    }

    // ─── 申请罚没 ─────────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long processForfeit(DepositForfeitDTO dto) {
        FinDepositAccount account = getAccountOrThrow(dto.getContractId());
        checkBalance(account, dto.getAmount(), "罚没");

        String code = "FORFEIT-" + dto.getContractId() + "-" + System.currentTimeMillis();
        FinDepositTransaction tx = buildTx(account, TRANS_FORFEIT, dto.getAmount(), null, code, dto.getReason(), 0);
        transactionMapper.insert(tx);

        submitApproval(tx, "FIN_DEPOSIT_OP", "保证金罚没-" + code);
        return tx.getId();
    }

    // ─── 审批回调（核心事务）────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveCallback(String approvalId, boolean approved) {
        // 1. 加锁查询流水
        FinDepositTransaction tx = transactionMapper.selectByApprovalIdForUpdate(approvalId);
        if (tx == null) {
            throw new FinBizException(FinErrorCode.FIN_4009, "找不到保证金流水，approvalId=" + approvalId);
        }
        // 幂等：已处理则忽略
        if (tx.getStatus() != 0) {
            log.warn("[保证金回调] 流水 id={} 已处理（status={}），忽略重复回调", tx.getId(), tx.getStatus());
            return;
        }

        if (!approved) {
            tx.setStatus(2); // 驳回
            transactionMapper.updateById(tx);
            return;
        }

        // 2. 加锁账户，二次校验余额
        FinDepositAccount account = baseMapper.selectByIdForUpdate(tx.getAccountId());
        if (account == null) throw new BizException("保证金账户不存在");
        checkBalance(account, tx.getAmount(), "审批生效");

        // 3. 扣减余额，更新累计字段
        BigDecimal newBalance = account.getBalance().subtract(tx.getAmount());
        account.setBalance(newBalance);

        switch (tx.getTransType()) {
            case TRANS_OFFSET  -> account.setTotalOffset((account.getTotalOffset() == null ? BigDecimal.ZERO : account.getTotalOffset()).add(tx.getAmount()));
            case TRANS_REFUND  -> account.setTotalRefund((account.getTotalRefund() == null ? BigDecimal.ZERO : account.getTotalRefund()).add(tx.getAmount()));
            case TRANS_FORFEIT -> account.setTotalForfeit((account.getTotalForfeit() == null ? BigDecimal.ZERO : account.getTotalForfeit()).add(tx.getAmount()));
        }

        int updated = baseMapper.updateById(account);
        if (updated == 0) throw new FinBizException(FinErrorCode.FIN_5001, "保证金账户乐观锁冲突");

        // 4. 更新流水：余额快照、状态
        tx.setBalanceAfter(newBalance);
        tx.setStatus(1);
        transactionMapper.updateById(tx);

        // 5. 若是冲抵，同步更新应收台账
        if (tx.getTransType() == TRANS_OFFSET) {
            syncReceivableOffset(tx);
        }

        log.info("[保证金回调] 流水 id={} 审批通过，类型={}，金额={}，余额→{}",
                tx.getId(), tx.getTransType(), tx.getAmount(), newBalance);
    }

    // ─── 私有辅助：冲抵时同步应收 ─────────────────────────────────────────────
    private void syncReceivableOffset(FinDepositTransaction tx) {
        // sourceCode 格式：REC:{receivableId}|{code}
        String src = tx.getSourceCode();
        if (src == null || !src.startsWith("REC:")) return;
        try {
            long receivableId = Long.parseLong(src.substring(4, src.indexOf('|')));
            FinReceivable receivable = receivableMapper.selectByIdForUpdate(receivableId);
            if (receivable == null) return;

            BigDecimal newReceived = receivable.getReceivedAmount().add(tx.getAmount());
            BigDecimal newOutstanding = receivable.getActualAmount().subtract(newReceived).max(BigDecimal.ZERO);
            receivable.setReceivedAmount(newReceived);
            receivable.setOutstandingAmount(newOutstanding);
            receivable.setStatus(newOutstanding.compareTo(BigDecimal.ZERO) <= 0 ? 2 : 1);
            receivableMapper.updateById(receivable);
            log.info("[保证金冲抵] 应收 {} 已更新，received={}", receivableId, newReceived);
        } catch (Exception e) {
            log.warn("[保证金冲抵] 更新应收失败：{}", e.getMessage());
        }
    }

    // ─── 私有辅助：余额校验 ─────────────────────────────────────────────────────
    private void checkBalance(FinDepositAccount account, BigDecimal required, String opName) {
        BigDecimal balance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
        if (balance.compareTo(required) < 0) {
            throw new FinBizException(FinErrorCode.FIN_4002,
                    String.format("%s金额 %.2f 超过可用余额 %.2f", opName, required, balance));
        }
    }

    // ─── 私有辅助：提交 OA 审批 ───────────────────────────────────────────────
    private void submitApproval(FinDepositTransaction tx, String bizType, String title) {
        try {
            String approvalId = oaApprovalAdapter.submitApproval(bizType, tx.getId(), title);
            tx.setApprovalId(approvalId);
            transactionMapper.updateById(tx);
        } catch (Exception e) {
            log.warn("[保证金] OA提交失败，流水 id={} 将手动推进", tx.getId(), e);
        }
    }

    // ─── 私有辅助：查账户或抛异常 ────────────────────────────────────────────
    private FinDepositAccount getAccountOrThrow(Long contractId) {
        FinDepositAccount account = baseMapper.selectByContractId(contractId);
        if (account == null) {
            throw new BizException("合同 " + contractId + " 尚无保证金账户，请先缴纳保证金");
        }
        return account;
    }

    // ─── 私有辅助：查找或初始化账户（合同info从DB补充） ───────────────────────
    private FinDepositAccount createAccount(Long contractId) {
        FinDepositAccount account = new FinDepositAccount();
        account.setContractId(contractId);
        account.setBalance(BigDecimal.ZERO);
        account.setTotalIn(BigDecimal.ZERO);
        account.setTotalOffset(BigDecimal.ZERO);
        account.setTotalRefund(BigDecimal.ZERO);
        account.setTotalForfeit(BigDecimal.ZERO);
        // 从合同补充商家/项目信息
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

    // ─── 私有辅助：构建流水对象 ─────────────────────────────────────────────────
    private FinDepositTransaction buildTx(FinDepositAccount account, int transType,
                                          BigDecimal amount, BigDecimal balanceAfter,
                                          String sourceCode, String reason, int status) {
        FinDepositTransaction tx = new FinDepositTransaction();
        tx.setAccountId(account.getId());
        tx.setTransType(transType);
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setTransDate(LocalDate.now());
        tx.setSourceCode(sourceCode);
        tx.setReason(reason);
        tx.setStatus(status);
        return tx;
    }

    // ─── 私有辅助：转换账户 VO ────────────────────────────────────────────────
    private DepositAccountVO toAccountVO(FinDepositAccount account) {
        DepositAccountVO vo = new DepositAccountVO();
        vo.setId(account.getId());
        vo.setContractId(account.getContractId());
        vo.setMerchantId(account.getMerchantId());
        vo.setProjectId(account.getProjectId());
        vo.setFeeItemId(account.getFeeItemId());
        vo.setBalance(account.getBalance() == null ? BigDecimal.ZERO : account.getBalance());
        vo.setTotalIn(account.getTotalIn() == null ? BigDecimal.ZERO : account.getTotalIn());
        vo.setTotalOffset(account.getTotalOffset() == null ? BigDecimal.ZERO : account.getTotalOffset());
        vo.setTotalRefund(account.getTotalRefund() == null ? BigDecimal.ZERO : account.getTotalRefund());
        vo.setTotalForfeit(account.getTotalForfeit() == null ? BigDecimal.ZERO : account.getTotalForfeit());
        vo.setCreateTime(account.getCreatedAt());
        vo.setUpdateTime(account.getUpdatedAt());

        // 补充关联名称
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
            log.debug("[保证金VO] 补充关联名称异常：{}", e.getMessage());
        }

        return vo;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}
