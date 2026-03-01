package com.asset.system.common.datascope;

import lombok.Data;

import java.util.List;

/**
 * 当前请求解析出的数据权限信息
 * <ul>
 *   <li>{@code admin=true} → 不限制（超管或全部数据权限）</li>
 *   <li>{@code admin=false, selfOnly=true} → 仅查 created_by=userId 的记录</li>
 *   <li>{@code admin=false, selfOnly=false, deptIds!=null} → dept_id IN(deptIds) 过滤</li>
 *   <li>{@code admin=false, selfOnly=false, deptIds==null} → 无限制（fallback）</li>
 * </ul>
 */
@Data
public class DataScopeInfo {
    /** 当前用户ID */
    private Long userId;
    /** 是否为超级管理员或拥有全部数据权限（不过滤）*/
    private boolean admin;
    /** 是否仅查看本人数据 */
    private boolean selfOnly;
    /** 允许访问的部门ID集合（null=不限，empty=无权限）*/
    private List<Long> deptIds;

    /** 快速构建：全部数据（不限制）*/
    public static DataScopeInfo ofAdmin(Long userId) {
        DataScopeInfo info = new DataScopeInfo();
        info.setUserId(userId);
        info.setAdmin(true);
        return info;
    }

    /** 快速构建：仅本人 */
    public static DataScopeInfo ofSelf(Long userId) {
        DataScopeInfo info = new DataScopeInfo();
        info.setUserId(userId);
        info.setSelfOnly(true);
        return info;
    }

    /** 快速构建：指定部门列表 */
    public static DataScopeInfo ofDepts(Long userId, List<Long> deptIds) {
        DataScopeInfo info = new DataScopeInfo();
        info.setUserId(userId);
        info.setDeptIds(deptIds);
        return info;
    }
}
