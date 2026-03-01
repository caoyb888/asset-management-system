package com.asset.system.post.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 岗位表 sys_post */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_post")
public class SysPost extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 岗位编码 */
    private String postCode;

    /** 岗位名称 */
    private String postName;

    /** 排序 */
    private Integer sortOrder;

    /** 状态: 0停用 1正常 */
    private Integer status;

    /** 备注 */
    private String remark;
}
