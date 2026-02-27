package com.asset.operation.alert.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 预警记录分页查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlertQueryDTO extends PageQuery {

    /** 预警类型（1合同到期/2应收到期），为 null 表示不过滤 */
    private Integer alertType;

    /** 发送状态（0待发送/1已发送/2发送失败/3已取消），为 null 表示不过滤 */
    private Integer sentStatus;

    /** 预警目标ID，为 null 表示不过滤 */
    private Long targetId;

    /** 预警日期范围-起始 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate alertDateFrom;

    /** 预警日期范围-截止 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate alertDateTo;
}
