package com.asset.system.common.exception;
import com.asset.common.exception.BizException;
/** 系统管理模块业务异常 */
public class SysBizException extends BizException {
    public SysBizException(SysErrorCode errorCode) { super(errorCode.getCode(), errorCode.getMessage()); }
    public SysBizException(SysErrorCode errorCode, String detail) { super(errorCode.getCode(), errorCode.getMessage() + "：" + detail); }
}
