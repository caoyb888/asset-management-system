package com.asset.base.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 品牌联系人 DTO（嵌入 BrandSaveDTO 或独立 CRUD 使用）
 */
@Data
public class BrandContactDTO {

    /** 联系人ID（编辑时传入，新增时为空） */
    private Long id;

    /** 联系人姓名（必填） */
    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    /** 电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 职位 */
    private String position;

    /** 是否主要联系人：0否 1是 */
    private Integer isPrimary;
}
