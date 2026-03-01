package com.asset.system.category.service;

import com.asset.system.category.dto.CategoryCreateDTO;
import com.asset.system.category.dto.CategoryQueryDTO;
import com.asset.system.category.dto.CategoryTreeVO;
import com.asset.system.category.entity.SysCategory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** 系统分类 Service */
public interface SysCategoryService extends IService<SysCategory> {

    /** 查询树形结构（按维度） */
    List<CategoryTreeVO> treeQuery(CategoryQueryDTO query);

    /** 新增分类节点 */
    Long createCategory(CategoryCreateDTO dto);

    /** 更新分类节点 */
    void updateCategory(CategoryCreateDTO dto);

    /** 删除分类节点（不允许删除有子节点的节点） */
    void deleteCategory(Long id);

    /** 启用/停用 */
    void changeStatus(Long id, Integer status);

    /**
     * 查询所有维度类型列表（去重）
     * 用于前端下拉切换分类维度
     */
    List<String> listCategoryTypes();
}
