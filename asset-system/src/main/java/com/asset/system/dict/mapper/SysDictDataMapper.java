package com.asset.system.dict.mapper;

import com.asset.system.dict.entity.SysDictData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 字典数据 Mapper */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    @Select("SELECT * FROM sys_dict_data WHERE dict_type=#{dictType} AND is_deleted=0 AND status=1 ORDER BY sort_order ASC")
    List<SysDictData> selectByDictType(@Param("dictType") String dictType);
}
