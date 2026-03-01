package com.asset.system.common.datascope;

/**
 * 数据权限上下文 — 基于 ThreadLocal 传递当前请求的权限范围
 * <p>由 {@link DataScopeAspect} 在方法前写入，方法返回后清除；
 * Service 层查询时通过 {@link #get()} 读取并注入 SQL 条件。</p>
 */
public final class DataScopeContext {

    private static final ThreadLocal<DataScopeInfo> HOLDER = new ThreadLocal<>();

    private DataScopeContext() {}

    public static void set(DataScopeInfo info) {
        HOLDER.set(info);
    }

    public static DataScopeInfo get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 当前线程是否存在数据范围限制（非超管且有 deptIds 或 selfOnly）*/
    public static boolean isRestricted() {
        DataScopeInfo info = HOLDER.get();
        if (info == null || info.isAdmin()) return false;
        return info.isSelfOnly() || info.getDeptIds() != null;
    }
}
