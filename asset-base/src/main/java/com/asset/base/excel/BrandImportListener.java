package com.asset.base.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.asset.base.entity.BizBrand;
import com.asset.base.model.dto.BrandImportRow;
import com.asset.base.service.BizBrandService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 品牌 Excel 导入监听器
 */
@Slf4j
public class BrandImportListener extends AnalysisEventListener<BrandImportRow> {

    private final BizBrandService brandService;
    private final List<String> errors = new ArrayList<>();
    private int successCount = 0;
    /** 数据行索引，从第2行开始（第1行是表头） */
    private int rowIndex = 1;

    public BrandImportListener(BizBrandService brandService) {
        this.brandService = brandService;
    }

    @Override
    public void invoke(BrandImportRow row, AnalysisContext context) {
        rowIndex++;
        try {
            if (!StringUtils.hasText(row.getBrandNameCn())) {
                errors.add("第" + rowIndex + "行：品牌名称(中)不能为空");
                return;
            }
            BizBrand brand = new BizBrand();
            brand.setBrandCode(row.getBrandCode());
            brand.setBrandNameCn(row.getBrandNameCn());
            brand.setBrandNameEn(row.getBrandNameEn());
            brand.setFormatType(row.getFormatType());
            brand.setBrandLevel(row.getBrandLevel());
            brand.setCooperationType(row.getCooperationType());
            brand.setBusinessNature(row.getBusinessNature());
            brand.setPhone(row.getPhone());
            brand.setBrandIntro(row.getBrandIntro());
            brandService.save(brand);
            successCount++;
        } catch (Exception e) {
            log.warn("品牌导入第{}行失败: {}", rowIndex, e.getMessage());
            errors.add("第" + rowIndex + "行：" + e.getMessage());
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("品牌导入完成，成功{}条，失败{}条", successCount, errors.size());
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
