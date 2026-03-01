package com.asset.system.common.enums;
import lombok.Getter;
/** 数据权限范围 */
@Getter
public enum DataScope {
    ALL(1, "全部数据"),
    CUSTOM(2, "自定义"),
    DEPT(3, "本部门"),
    DEPT_AND_CHILD(4, "本部门及以下"),
    SELF(5, "仅本人");
    private final int code; private final String desc;
    DataScope(int code, String desc) { this.code = code; this.desc = desc; }
    public static DataScope of(int code) { for (DataScope v : values()) if (v.code == code) return v; return ALL; }
}
