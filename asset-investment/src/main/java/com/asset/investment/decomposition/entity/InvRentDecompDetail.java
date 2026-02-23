package com.asset.investment.decomposition.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/** 租金分解明细表实体 - 对应 inv_rent_decomp_detail 表 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_rent_decomp_detail")
public class InvRentDecompDetail extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long decompId;
    /** 商铺类别: 1主力/2次主力/3一般 */
    private Integer shopCategory;
    private String formatType;
    private BigDecimal rentUnitPrice;
    private BigDecimal propertyUnitPrice;
    private BigDecimal area;
    /** 标准年租金 = rent_unit_price × area × 12 */
    private BigDecimal annualRent;
    /** 标准年物管费 = property_unit_price × area × 12 */
    private BigDecimal annualFee;
    private String remark;
}
