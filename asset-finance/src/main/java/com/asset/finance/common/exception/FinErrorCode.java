package com.asset.finance.common.exception;

import lombok.Getter;

/**
 * 财务模块业务错误码枚举
 * 4xxx：业务规则错误（客户端可处理）
 * 5xxx：系统/并发错误（客户端重试或联系管理员）
 */
@Getter
public enum FinErrorCode {

    // ─── 4xxx 业务规则错误 ──────────────────────────────────
    FIN_4001(4001, "核销金额超过收款余额"),
    FIN_4002(4002, "保证金余额不足"),
    FIN_4003(4003, "收款单已核销，不可作废"),
    FIN_4004(4004, "应收金额不可为负"),
    FIN_4005(4005, "负数核销方向错误"),
    FIN_4006(4006, "凭证借贷不平衡"),
    FIN_4007(4007, "凭证已上传，不可重复"),
    FIN_4008(4008, "拆分明细合计不等于收款总额"),
    FIN_4009(4009, "审批单据状态异常，疑似重复回调"),

    // ─── 5xxx 系统/并发错误 ──────────────────────────────────
    FIN_5001(5001, "账务更新事务失败，请重试"),
    FIN_5002(5002, "并发更新冲突，请稍后重试");

    private final int code;
    private final String message;

    FinErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
