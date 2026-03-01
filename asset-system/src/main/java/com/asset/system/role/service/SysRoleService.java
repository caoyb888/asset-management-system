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
    /** 所有正常状态角色（下拉用）*/
    List<SysRole> listEnabled();
    RoleDetailVO getDetailById(Long id);
    Long createRole(RoleCreateDTO dto);
    void updateRole(RoleCreateDTO dto);
    void deleteRole(Long id);
    void changeStatus(Long id, Integer status);
    /** 分配菜单权限 */
    void grantMenus(Long roleId, List<Long> menuIds);
    /** 获取角色已分配菜单ID */
    List<Long> getMenuIds(Long roleId);
    /** 设置数据权限范围 */
    void setDataScope(Long roleId, Integer dataScope, List<Long> deptIds);
    /** 获取自定义数据权限部门ID */
    List<Long> getDeptIds(Long roleId);
}
