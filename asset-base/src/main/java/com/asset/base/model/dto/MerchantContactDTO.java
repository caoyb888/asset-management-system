package com.asset.base.model.dto;

import lombok.Data;

/**
 * 商家联系人 DTO（嵌入 MerchantSaveDTO）
 */
@Data
public class MerchantContactDTO {

    /** 联系人ID（编辑时传入，新增时为空） */
    private Long id;

    /** 联系人姓名 */
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
