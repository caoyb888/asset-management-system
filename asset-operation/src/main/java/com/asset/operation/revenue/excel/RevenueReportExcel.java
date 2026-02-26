package com.asset.operation.revenue.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 营收填报 Excel 行对象（导入/导出共用）
 * 导入格式：合同编号 + 填报日期 + 营业额
 * 导出格式：追加商铺编码、商家名称
 */
@Data
@ColumnWidth(20)
public class RevenueReportExcel {
    @ExcelProperty(value = "合同编号", index = 0)
    private String contractCode;

    @ExcelProperty(value = "商铺编号", index = 1)
    private String shopCode;

    @ExcelProperty(value = "商家名称", index = 2)
    private String merchantName;

    /** 填报日期，格式 YYYY-MM-DD */
    @ExcelProperty(value = "填报日期（YYYY-MM-DD）", index = 3)
    private String reportDate;

    /** 营业额（元） */
    @ExcelProperty(value = "营业额（元）", index = 4)
    private String revenueAmount;

    /** 错误原因（导入结果回写时使用，导出时为空） */
    @ExcelProperty(value = "错误原因", index = 5)
    private String errorMsg;
}
