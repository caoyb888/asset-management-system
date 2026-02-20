package com.asset.common.log.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解 - 标注在Controller方法上，自动采集操作日志
 * 使用: @OperLog(title = "合同管理", businessType = "新增")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {
    /** 模块名称 */
    String title() default "";
    /** 业务类型(新增/修改/删除/导出/审批等) */
    String businessType() default "";
    /** 是否保存请求参数 */
    boolean isSaveRequestData() default true;
    /** 是否保存响应数据 */
    boolean isSaveResponseData() default false;
}
