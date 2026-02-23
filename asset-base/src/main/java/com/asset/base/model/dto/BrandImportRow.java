package com.asset.base.model.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 品牌 Excel 导入行模型
 */
@Data
public class BrandImportRow {

    @ExcelProperty("品牌编码")
    private String brandCode;

    @ExcelProperty("品牌名称(中)")
    private String brandNameCn;

    @ExcelProperty("品牌名称(英)")
    private String brandNameEn;

    @ExcelProperty("所属业态")
    private String formatType;

    @ExcelProperty("品牌等级(1高端/2中端/3大众)")
    private Integer brandLevel;

    @ExcelProperty("合作关系(1直营/2加盟/3代理)")
    private Integer cooperationType;

    @ExcelProperty("经营性质(1餐饮/2零售/3娱乐/4服务)")
    private Integer businessNature;

    @ExcelProperty("联系电话")
    private String phone;

    @ExcelProperty("品牌简介")
    private String brandIntro;
}
