package com.asset.system.extfield.service;

import com.asset.system.extfield.dto.ExtFieldCreateDTO;
import com.asset.system.extfield.dto.ExtFieldSortItem;
import com.asset.system.extfield.dto.ExtFieldVO;
import com.asset.system.extfield.entity.SysExtFieldDef;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** 用户自定义扩展字段管理 Service */
public interface SysExtFieldService extends IService<SysExtFieldDef> {

    /** 查询指定模块的字段定义列表（按 sort_order 升序，走 Redis 缓存） */
    List<ExtFieldVO> listByModule(String moduleCode);

    /** 查询单个字段定义详情 */
    ExtFieldVO getById(Long id);

    /** 新增字段定义 */
    Long create(ExtFieldCreateDTO dto);

    /** 修改字段定义（fieldKey 不允许修改） */
    void update(ExtFieldCreateDTO dto);

    /** 逻辑删除字段定义 */
    void delete(Long id);

    /** 批量更新排序 */
    void updateSort(List<ExtFieldSortItem> items);
}
