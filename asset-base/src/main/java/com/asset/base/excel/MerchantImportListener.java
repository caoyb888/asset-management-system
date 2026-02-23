package com.asset.base.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.asset.base.entity.BizMerchant;
import com.asset.base.model.dto.MerchantImportRow;
import com.asset.base.service.BizMerchantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家 Excel 导入监听器
 */
@Slf4j
public class MerchantImportListener extends AnalysisEventListener<MerchantImportRow> {

    private final BizMerchantService merchantService;
    /** 必须指定归属项目 */
    private final Long projectId;
    private final List<String> errors = new ArrayList<>();
    private int successCount = 0;
    /** 数据行索引，从第2行开始（第1行是表头） */
    private int rowIndex = 1;

    public MerchantImportListener(BizMerchantService merchantService, Long projectId) {
        this.merchantService = merchantService;
        this.projectId = projectId;
    }

    @Override
    public void invoke(MerchantImportRow row, AnalysisContext context) {
        rowIndex++;
        try {
            if (!StringUtils.hasText(row.getMerchantName())) {
                errors.add("第" + rowIndex + "行：商家名称不能为空");
                return;
            }
            BizMerchant merchant = new BizMerchant();
            merchant.setProjectId(projectId);
            merchant.setMerchantCode(row.getMerchantCode());
            merchant.setMerchantName(row.getMerchantName());
            merchant.setMerchantAttr(row.getMerchantAttr());
            merchant.setMerchantNature(row.getMerchantNature());
            merchant.setFormatType(row.getFormatType());
            merchant.setNaturalPerson(row.getNaturalPerson());
            merchant.setAddress(row.getAddress());
            merchant.setPhone(row.getPhone());
            merchant.setMerchantLevel(row.getMerchantLevel() != null ? row.getMerchantLevel() : 3);
            merchant.setAuditStatus(0);
            merchantService.save(merchant);
            successCount++;
        } catch (Exception e) {
            log.warn("商家导入第{}行失败: {}", rowIndex, e.getMessage());
            errors.add("第" + rowIndex + "行：" + e.getMessage());
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("商家导入完成，成功{}条，失败{}条", successCount, errors.size());
    }

    /**
     * 获取导入结果统计
     */
    public Map<String, Object> getResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", errors.size());
        result.put("errors", errors);
        return result;
    }
}
