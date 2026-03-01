package com.asset.system.log.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 操作日志查询参数 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperLogQueryDTO extends PageQuery {
    private String module;
    private String operUser;
    private Integer status;
    private LocalDateTime timeFrom;
    private LocalDateTime timeTo;
}
