package com.asset.system.dict.mapper;

import com.asset.system.dict.entity.SysDictData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 字典数据 Mapper */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    /** 查询启用状态字典数据（前端业务下拉用，走缓存） */
    @Select("SELECT * FROM sys_dict_data WHERE dict_type=#{dictType} AND is_deleted=0 AND status=1 ORDER BY sort_order ASC")
    List<SysDictData> selectByDictType(@Param("dictType") String dictType);

    /** 查询全部字典数据（含停用，管理界面用） */
    @Select("SELECT * FROM sys_dict_data WHERE dict_type=#{dictType} AND is_deleted=0 ORDER BY sort_order ASC")
    List<SysDictData> selectAllByDictType(@Param("dictType") String dictType);

    /** 逻辑删除指定字典类型下所有数据（级联删除用） */
    @Update("UPDATE sys_dict_data SET is_deleted=1 WHERE dict_type=#{dictType} AND is_deleted=0")
    int logicDeleteByDictType(@Param("dictType") String dictType);
}
