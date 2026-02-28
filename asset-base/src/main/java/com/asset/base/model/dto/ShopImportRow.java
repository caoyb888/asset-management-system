package com.asset.base.model.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商铺 Excel 导入行模型
 */
@Data
public class ShopImportRow {

    @ExcelProperty("项目编码")
    private String projectCode;

    @ExcelProperty("楼栋编码")
    private String buildingCode;

    @ExcelProperty("楼层编码")
    private String floorCode;

    @ExcelProperty("铺位号")
    private String shopCode;

    @ExcelProperty("商铺类型(1临街/2内铺/3专柜)")
    private Integer shopType;

    @ExcelProperty("计租面积(㎡)")
    private BigDecimal rentArea;

    @ExcelProperty("实测面积(㎡)")
    private BigDecimal measuredArea;

    @ExcelProperty("建筑面积(㎡)")
    private BigDecimal buildingArea;

    @ExcelProperty("经营面积(㎡)")
    private BigDecimal operatingArea;

    @ExcelProperty("商铺状态(0空置/1在租/2自用/3预留)")
    private Integer shopStatus;

    @ExcelProperty("规划业态")
    private String plannedFormat;

    @ExcelProperty("签约业态")
    private String signedFormat;

    @ExcelProperty("业主名称")
    private String ownerName;

    @ExcelProperty("业主联系人")
    private String ownerContact;

    @ExcelProperty("业主电话")
    private String ownerPhone;
}
