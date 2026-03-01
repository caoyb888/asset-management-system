package com.asset.system.common.enums;
import lombok.Getter;
/** 菜单类型 */
@Getter
public enum MenuType {
    DIRECTORY("M", "目录"), MENU("C", "菜单"), BUTTON("F", "按钮/权限");
    private final String code; private final String desc;
    MenuType(String code, String desc) { this.code = code; this.desc = desc; }
    public static MenuType of(String code) { for (MenuType v : values()) if (v.code.equals(code)) return v; return null; }
}
