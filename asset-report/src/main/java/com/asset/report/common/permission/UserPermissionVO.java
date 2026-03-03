package com.asset.report.common.permission;

import lombok.Data;

import java.util.List;

/**
 * 当前用户报表权限 VO
 * <p>
 * 接口 GET /rpt/common/user-permissions 返回，供前端控制菜单可见性和数据展示方式。
 * </p>
 */
@Data
public class UserPermissionVO {

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 是否为管理员（data_scope=1 或 username=admin） */
    private boolean admin;

    /**
     * 是否有财务绝对金额查看权限
     * <p>
     * true：可查看应收/已收/欠款等绝对金额；<br>
     * false：财务绝对金额脱敏，仅可查看同比/环比趋势数据。
     * </p>
     */
    private boolean hasFinViewPerm;

    /**
     * 可访问的报表模块列表
     * <p>
     * 取值：ASSET（资产）、INV（招商）、OPR（营运）、FIN（财务）
     * </p>
     */
    private List<String> accessibleModules;

    /**
     * 当前用户可见的项目 ID 列表
     * <p>null 表示管理员，可见全部项目</p>
     */
    private List<Long> permittedProjectIds;
}
