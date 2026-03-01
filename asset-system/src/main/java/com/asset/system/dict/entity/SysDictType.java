package com.asset.system.dict.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 字典类型表 sys_dict_type */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典名称 */
    private String dictName;

    /** 字典类型标识 */
    private String dictType;

    /** 状态: 0停用 1正常 */
    private Integer status;

    /** 备注 */
    private String remark;
}
