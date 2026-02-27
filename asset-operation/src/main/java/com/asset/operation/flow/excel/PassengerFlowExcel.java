package com.asset.operation.flow.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 客流填报 Excel 行对象（导入/导出共用）
 * 导入格式：项目编号 + 填报日期 + 客流人数
 * 导出格式：追加楼栋编号、楼层编号
 */
@Data
@ColumnWidth(20)
public class PassengerFlowExcel {

    @ExcelProperty(value = "项目编号", index = 0)
    private String projectCode;

    @ExcelProperty(value = "楼栋编号（选填）", index = 1)
    private String buildingCode;

    @ExcelProperty(value = "楼层编号（选填）", index = 2)
    private String floorCode;

    /** 填报日期，格式 YYYY-MM-DD */
    @ExcelProperty(value = "填报日期（YYYY-MM-DD）", index = 3)
    private String reportDate;

    /** 客流人数 */
    @ExcelProperty(value = "客流人数", index = 4)
    private String flowCount;

    /** 错误原因（导入结果回写时使用，导出时为空） */
    @ExcelProperty(value = "错误原因", index = 5)
    private String errorMsg;
}
