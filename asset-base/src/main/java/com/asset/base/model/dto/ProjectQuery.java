package com.asset.base.model.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目列表查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectQuery extends PageQuery {

    /** 项目名称（模糊） */
    private String projectName;

    /** 项目编号（模糊） */
    private String projectCode;

    /** 运营状态：0筹备 1开业 2停业 */
    private Integer operationStatus;

    /** 所在省份 */
    private String province;

    /** 所在城市 */
    private String city;

    /** 所属公司ID */
    private Long companyId;
}
