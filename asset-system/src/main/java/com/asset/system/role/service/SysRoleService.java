package com.asset.system.role.service;

import com.asset.system.role.dto.RoleCreateDTO;
import com.asset.system.role.dto.RoleDetailVO;
import com.asset.system.role.dto.RoleQueryDTO;
import com.asset.system.role.entity.SysRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** 角色管理 Service */
public interface SysRoleService extends IService<SysRole> {
    IPage<SysRole> pageQuery(RoleQueryDTO query);
    RoleDetailVO getDetailById(Long id);
    Long createRole(RoleCreateDTO dto);
    void updateRole(RoleCreateDTO dto);
    void deleteRole(Long id);
    void changeStatus(Long id, Integer status);
    /** 分配菜单权限 */
    void grantMenus(Long roleId, List<Long> menuIds);
}
