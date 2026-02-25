package com.asset.investment.decomposition.excel;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 租金分解明细 Excel 行模型
 * 导入：用于从 Excel 批量读取明细数据
 * 导出：用于生成 Excel 明细报表
 */
@Data
@ExcelIgnoreUnannotated
public class RentDecompDetailExcel {

    @ExcelProperty("商铺类别(1主力/2次主力/3一般)")
    @ColumnWidth(22)
    private Integer shopCategory;

    @ExcelProperty("业态")
    @ColumnWidth(20)
    private String formatType;

    @ExcelProperty("租金单价(元/㎡/月)")
    @ColumnWidth(20)
    private BigDecimal rentUnitPrice;

    @ExcelProperty("物管费单价(元/㎡/月)")
    @ColumnWidth(22)
    private BigDecimal propertyUnitPrice;

    @ExcelProperty("面积(㎡)")
    @ColumnWidth(14)
    private BigDecimal area;

    @ExcelProperty("标准年租金(元)")
    @ColumnWidth(18)
    private BigDecimal annualRent;

    @ExcelProperty("标准年物管费(元)")
    @ColumnWidth(18)
    private BigDecimal annualFee;

    @ExcelProperty("备注")
    @ColumnWidth(24)
    private String remark;
}
