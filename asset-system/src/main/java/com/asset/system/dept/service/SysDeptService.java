package com.asset.system.dept.service;

import com.asset.system.dept.dto.DeptCreateDTO;
import com.asset.system.dept.dto.DeptTreeVO;
import com.asset.system.dept.dto.MoveDeptDTO;
import com.asset.system.dept.entity.SysDept;
import com.asset.system.user.dto.UserDetailVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** 部门/机构管理 Service */
public interface SysDeptService extends IService<SysDept> {

    /** 获取部门树（按状态过滤），优先读 Redis 缓存 */
    List<DeptTreeVO> getDeptTree(Integer status);

    /** 获取部门详情 */
    SysDept getDetailById(Long id);

    /** 新增部门 */
    Long createDept(DeptCreateDTO dto);

    /** 更新部门（若上级变更则同步更新当前节点及所有后代的 ancestors） */
    void updateDept(DeptCreateDTO dto);

    /** 删除部门（不可删除有子部门或有用户的部门） */
    void deleteDept(Long id);

    /** 修改状态 */
    void changeStatus(Long id, Integer status);

    /** 移动子树：变更上级机构，批量更新后代 ancestors */
    void moveDept(Long id, MoveDeptDTO dto);

    /** 查询部门下的用户列表（含直属子部门用户） */
    List<UserDetailVO> getDeptUsers(Long deptId, boolean includeChildren);

    /** 获取部门的祖级路径列表 */
    String buildAncestors(Long parentId);

    /** 清除机构树 Redis 缓存（增删改后调用） */
    void evictCache();
}
