package com.asset.system.dict.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 字典数据表 sys_dict_data */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型标识 */
    private String dictType;

    /** 字典标签 */
    private String dictLabel;

    /** 字典值 */
    private String dictValue;

    /** 样式属性（el-tag type: primary/success/info/warning/danger） */
    private String cssClass;

    /** 排序 */
    private Integer sortOrder;

    /** 状态: 0停用 1正常 */
    private Integer status;

    /** 备注 */
    private String remark;
}
