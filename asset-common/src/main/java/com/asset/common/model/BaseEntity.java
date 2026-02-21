package com.asset.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类 - 包含逻辑删除 + 审计五件套
 * <p>
 * 所有业务表均继承此类，MyBatis-Plus 自动注入：
 *   is_deleted  → @TableLogic
 *   created_by  → MetaObjectHandler INSERT 填充
 *   created_at  → MetaObjectHandler INSERT 填充
 *   updated_by  → MetaObjectHandler INSERT_UPDATE 填充
 *   updated_at  → MetaObjectHandler INSERT_UPDATE 填充
 * </p>
 */
@Data
public abstract class BaseEntity implements Serializable {

    /** 逻辑删除：0正常 1删除 */
    @TableLogic
    private Integer isDeleted;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新人ID */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
