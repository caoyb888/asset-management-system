package com.asset.system.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 用户新增/更新请求 */
@Data
public class UserCreateDTO {
    /** 用户ID（更新时必填） */
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度2-50位")
    private String username;

    /** 密码（新增必填，更新不填则不修改） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 所属部门ID */
    private Long deptId;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像URL */
    private String avatar;

    /** 状态: 0停用 1正常 */
    private Integer status;

    /** 角色ID列表 */
    private List<Long> roleIds;

    /** 岗位ID列表 */
    private List<Long> postIds;
}
