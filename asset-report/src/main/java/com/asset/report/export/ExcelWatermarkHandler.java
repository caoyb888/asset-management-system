package com.asset.report.export;

import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Excel 导出水印处理器
 * <p>
 * 在每个 Sheet 的打印页眉/页脚中嵌入导出人信息，
 * 作为导出文件的溯源水印，满足数据安全合规要求。
 * </p>
 *
 * <h3>水印内容</h3>
 * <ul>
 *   <li>页眉左侧：【内部资料，请勿外传】</li>
 *   <li>页眉右侧：导出人 + 导出时间</li>
 *   <li>页脚居中：由资产管理系统自动生成</li>
 * </ul>
 */
@RequiredArgsConstructor
public class ExcelWatermarkHandler implements SheetWriteHandler {

    private final String exportUser;
    private final String exportTime;

    @Override
    public void afterSheetCreate(WriteWorkbookHolder workbookHolder, WriteSheetHolder sheetHolder) {
        Sheet sheet = sheetHolder.getSheet();

        // 页眉：右侧显示导出人和导出时间（打印时可见）
        Header header = sheet.getHeader();
        header.setLeft("【内部资料，请勿外传】");
        header.setRight("导出人：" + exportUser + "  导出时间：" + exportTime);

        // 页脚：居中显示系统来源
        Footer footer = sheet.getFooter();
        footer.setCenter("本报表由资产管理系统自动生成，仅供内部使用");
    }
}
