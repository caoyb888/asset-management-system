package com.asset.finance.common.exception;

import com.asset.common.exception.BizException;

/**
 * 财务模块业务异常
 *
 * <p>继承 {@link BizException}，由公共模块的 GlobalExceptionHandler 统一捕获，
 * 返回格式：{code: 财务错误码, msg: 错误描述}
 */
public class FinBizException extends BizException {

    public FinBizException(FinErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }

    public FinBizException(FinErrorCode errorCode, String detail) {
        super(errorCode.getCode(), errorCode.getMessage() + "：" + detail);
    }
}
