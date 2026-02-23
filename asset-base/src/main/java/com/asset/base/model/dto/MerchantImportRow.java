package com.asset.base.model.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 商家 Excel 导入行模型
 */
@Data
public class MerchantImportRow {

    @ExcelProperty("商家编号")
    private String merchantCode;

    @ExcelProperty("商家名称")
    private String merchantName;

    @ExcelProperty("商家属性(1个体户/2企业)")
    private Integer merchantAttr;

    @ExcelProperty("商家性质(1民营/2国营/3外资/4合资)")
    private Integer merchantNature;

    @ExcelProperty("经营业态")
    private String formatType;

    @ExcelProperty("自然人姓名")
    private String naturalPerson;

    @ExcelProperty("地址")
    private String address;

    @ExcelProperty("手机")
    private String phone;

    @ExcelProperty("商家评级(1优秀/2良好/3一般/4差)")
    private Integer merchantLevel;
}
