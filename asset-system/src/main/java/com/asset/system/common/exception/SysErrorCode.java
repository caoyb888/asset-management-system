package com.asset.system.common.exception;
import lombok.Getter;
/** 系统管理模块错误码（6xxx） */
@Getter
public enum SysErrorCode {
    // 用户相关
    USER_NOT_FOUND(6001, "用户不存在"),
    USER_ALREADY_EXISTS(6002, "用户名已存在"),
    USER_DISABLED(6003, "用户已停用"),
    USER_DELETE_SELF_FORBIDDEN(6004, "不能删除当前登录用户"),
    USER_HAS_ADMIN_ROLE(6005, "超级管理员不允许操作"),
    // 部门相关
    DEPT_NOT_FOUND(6011, "部门不存在"),
    DEPT_HAS_CHILDREN(6012, "存在子部门，不允许删除"),
    DEPT_HAS_USERS(6013, "部门下存在用户，不允许删除"),
    // 岗位相关
    POST_NOT_FOUND(6021, "岗位不存在"),
    POST_CODE_EXISTS(6022, "岗位编码已存在"),
    POST_HAS_USERS(6023, "岗位下存在用户，不允许删除"),
    // 角色相关
    ROLE_NOT_FOUND(6031, "角色不存在"),
    ROLE_CODE_EXISTS(6032, "角色编码已存在"),
    ROLE_IS_ADMIN(6033, "超级管理员角色不允许修改"),
    // 菜单相关
    MENU_NOT_FOUND(6041, "菜单不存在"),
    MENU_HAS_CHILDREN(6042, "存在子菜单，不允许删除"),
    // 字典相关
    DICT_TYPE_NOT_FOUND(6051, "字典类型不存在"),
    DICT_TYPE_EXISTS(6052, "字典类型标识已存在"),
    // 通用
    SYS_5001(6099, "系统操作失败");

    private final int code; private final String message;
    SysErrorCode(int code, String message) { this.code = code; this.message = message; }
}
