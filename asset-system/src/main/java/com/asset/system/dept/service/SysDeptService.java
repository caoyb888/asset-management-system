package com.asset.system.dept.service;

import com.asset.system.dept.dto.DeptCreateDTO;
import com.asset.system.dept.dto.DeptTreeVO;
import com.asset.system.dept.entity.SysDept;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** 部门/机构管理 Service */
public interface SysDeptService extends IService<SysDept> {

    /** 获取部门树（按状态过滤） */
    List<DeptTreeVO> getDeptTree(Integer status);

    /** 获取部门详情 */
    SysDept getDetailById(Long id);

    /** 新增部门 */
    Long createDept(DeptCreateDTO dto);

    /** 更新部门 */
    void updateDept(DeptCreateDTO dto);

    /** 删除部门（不可删除有子部门或有用户的部门） */
    void deleteDept(Long id);

    /** 修改状态 */
    void changeStatus(Long id, Integer status);

    /** 获取部门的祖级路径列表 */
    String buildAncestors(Long parentId);
}
