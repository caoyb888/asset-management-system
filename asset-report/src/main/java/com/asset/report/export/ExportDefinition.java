package com.asset.report.export;

import java.util.List;

/**
 * 报表导出定义（数据 + 列头 + Sheet 名称）
 *
 * @param sheetName Sheet 名称（中文）
 * @param headers   EasyExcel 列头，每个内层 List 代表一列（多级列头支持）
 * @param dataRows  数据行，每行为 List&lt;Object&gt;，顺序与 headers 对应
 */
public record ExportDefinition(
        String sheetName,
        List<List<String>> headers,
        List<List<Object>> dataRows
) {}
