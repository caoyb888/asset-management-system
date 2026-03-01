package com.asset.system.user.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 用户详情 VO */
@Data
public class UserDetailVO {
    private Long id;
    private String username;
    private String realName;
    private Long deptId;
    private String deptName;
    private String phone;
    private String email;
    private String avatar;
    private Integer status;
    private String statusName;
    private String loginIp;
    private LocalDateTime loginTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 拥有的角色ID列表 */
    private List<Long> roleIds;
    /** 拥有的角色名称列表 */
    private List<String> roleNames;
    /** 拥有的岗位ID列表 */
    private List<Long> postIds;
    /** 拥有的岗位名称列表 */
    private List<String> postNames;
}
