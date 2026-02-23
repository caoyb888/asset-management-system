package com.asset.base.controller;

import com.alibaba.excel.EasyExcel;
import com.asset.base.entity.BizBrand;
import com.asset.base.entity.BizBrandContact;
import com.asset.base.excel.BrandImportListener;
import com.asset.base.mapper.BizBrandContactMapper;
import com.asset.base.model.dto.BrandContactDTO;
import com.asset.base.model.dto.BrandImportRow;
import com.asset.base.model.dto.BrandQuery;
import com.asset.base.model.dto.BrandSaveDTO;
import com.asset.base.model.vo.BrandContactVO;
import com.asset.base.model.vo.BrandVO;
import com.asset.base.service.BizBrandService;
import com.asset.common.exception.BizException;
import com.asset.common.log.annotation.OperLog;
import com.asset.common.model.R;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 品牌管理 Controller
 *
 * <pre>
 * GET    /base/brands                     分页查询
 * POST   /base/brands                     新增品牌
 * GET    /base/brands/{id}                品牌详情
 * PUT    /base/brands/{id}                编辑品牌
 * DELETE /base/brands/{id}                逻辑删除
 * GET    /base/brands/{id}/contacts       品牌联系人列表
 * POST   /base/brands/{id}/contacts       新增联系人
 * PUT    /base/brands/{id}/contacts/{cid} 编辑联系人
 * DELETE /base/brands/{id}/contacts/{cid} 删除联系人
 * POST   /base/brands/import              批量导入
 * GET    /base/brands/template            下载导入模板
 * </pre>
 */
@Tag(name = "品牌管理", description = "基础数据-品牌增删改查")
@RestController
@RequestMapping("/base/brands")
@RequiredArgsConstructor
public class BizBrandController {

    private final BizBrandService brandService;
    private final BizBrandContactMapper contactMapper;

    /* ================================================================== */
    /* 品牌基础 CRUD                                                          */
    /* ================================================================== */

    @Operation(summary = "分页查询品牌列表")
    @GetMapping
    @OperLog(module = "品牌管理", action = "分页查询", type = OperLog.OperType.QUERY)
    public R<IPage<BrandVO>> page(BrandQuery query) {
        return R.ok(brandService.pageBrand(query));
    }

    @Operation(summary = "查询品牌详情")
    @GetMapping("/{id}")
    @OperLog(module = "品牌管理", action = "查询详情", type = OperLog.OperType.QUERY)
    public R<BrandVO> detail(
            @Parameter(description = "品牌ID") @PathVariable Long id) {
        return R.ok(brandService.getBrandById(id));
    }

    @Operation(summary = "新增品牌")
    @PostMapping
    @OperLog(module = "品牌管理", action = "新增", type = OperLog.OperType.CREATE)
    public R<Long> create(@Valid @RequestBody BrandSaveDTO dto) {
        return R.ok(brandService.createBrand(dto));
    }

    @Operation(summary = "编辑品牌")
    @PutMapping("/{id}")
    @OperLog(module = "品牌管理", action = "编辑", type = OperLog.OperType.UPDATE)
    public R<Void> update(
            @Parameter(description = "品牌ID") @PathVariable Long id,
            @Valid @RequestBody BrandSaveDTO dto) {
        brandService.updateBrand(id, dto);
        return R.ok(null);
    }

    @Operation(summary = "删除品牌（逻辑删除）")
    @DeleteMapping("/{id}")
    @OperLog(module = "品牌管理", action = "删除", type = OperLog.OperType.DELETE)
    public R<Void> delete(
            @Parameter(description = "品牌ID") @PathVariable Long id) {
        brandService.deleteBrand(id);
        return R.ok(null);
    }

    /* ================================================================== */
    /* 品牌联系人独立 CRUD                                                    */
    /* ================================================================== */

    @Operation(summary = "查询品牌联系人列表")
    @GetMapping("/{id}/contacts")
    @OperLog(module = "品牌管理", action = "查询联系人", type = OperLog.OperType.QUERY)
    public R<List<BrandContactVO>> listContacts(
            @Parameter(description = "品牌ID") @PathVariable Long id) {
        // 校验品牌存在
        BizBrand brand = brandService.getById(id);
        if (brand == null || brand.getIsDeleted() == 1) {
            throw new BizException("品牌不存在或已删除");
        }
        List<BizBrandContact> list = contactMapper.selectList(
                new LambdaQueryWrapper<BizBrandContact>()
                        .eq(BizBrandContact::getBrandId, id)
                        .eq(BizBrandContact::getIsDeleted, 0)
                        .orderByDesc(BizBrandContact::getIsPrimary)
                        .orderByAsc(BizBrandContact::getCreatedAt)
        );
        List<BrandContactVO> voList = list.stream().map(c -> {
            BrandContactVO vo = new BrandContactVO();
            vo.setId(c.getId());
            vo.setBrandId(c.getBrandId());
            vo.setContactName(c.getContactName());
            vo.setPhone(c.getPhone());
            vo.setEmail(c.getEmail());
            vo.setPosition(c.getPosition());
            vo.setIsPrimary(c.getIsPrimary());
            vo.setIsPrimaryDesc(Integer.valueOf(1).equals(c.getIsPrimary()) ? "是" : "否");
            return vo;
        }).toList();
        return R.ok(voList);
    }

    @Operation(summary = "新增品牌联系人")
    @PostMapping("/{id}/contacts")
    @OperLog(module = "品牌管理", action = "新增联系人", type = OperLog.OperType.CREATE)
    public R<Long> addContact(
            @Parameter(description = "品牌ID") @PathVariable Long id,
            @Valid @RequestBody BrandContactDTO dto) {
        BizBrand brand = brandService.getById(id);
        if (brand == null || brand.getIsDeleted() == 1) {
            throw new BizException("品牌不存在或已删除");
        }
        BizBrandContact contact = new BizBrandContact();
        contact.setBrandId(id);
        contact.setContactName(dto.getContactName());
        contact.setPhone(dto.getPhone());
        contact.setEmail(dto.getEmail());
        contact.setPosition(dto.getPosition());
        contact.setIsPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : 0);
        contactMapper.insert(contact);
        return R.ok(contact.getId());
    }

    @Operation(summary = "编辑品牌联系人")
    @PutMapping("/{id}/contacts/{cid}")
    @OperLog(module = "品牌管理", action = "编辑联系人", type = OperLog.OperType.UPDATE)
    public R<Void> updateContact(
            @Parameter(description = "品牌ID") @PathVariable Long id,
            @Parameter(description = "联系人ID") @PathVariable Long cid,
            @Valid @RequestBody BrandContactDTO dto) {
        BizBrandContact contact = contactMapper.selectById(cid);
        if (contact == null || contact.getIsDeleted() == 1) {
            throw new BizException("联系人不存在或已删除");
        }
        if (!id.equals(contact.getBrandId())) {
            throw new BizException("联系人不属于该品牌");
        }
        contact.setContactName(dto.getContactName());
        contact.setPhone(dto.getPhone());
        contact.setEmail(dto.getEmail());
        contact.setPosition(dto.getPosition());
        if (dto.getIsPrimary() != null) {
            contact.setIsPrimary(dto.getIsPrimary());
        }
        contactMapper.updateById(contact);
        return R.ok(null);
    }

    @Operation(summary = "删除品牌联系人（逻辑删除）")
    @DeleteMapping("/{id}/contacts/{cid}")
    @OperLog(module = "品牌管理", action = "删除联系人", type = OperLog.OperType.DELETE)
    public R<Void> deleteContact(
            @Parameter(description = "品牌ID") @PathVariable Long id,
            @Parameter(description = "联系人ID") @PathVariable Long cid) {
        BizBrandContact contact = contactMapper.selectById(cid);
        if (contact == null || contact.getIsDeleted() == 1) {
            throw new BizException("联系人不存在或已删除");
        }
        if (!id.equals(contact.getBrandId())) {
            throw new BizException("联系人不属于该品牌");
        }
        contactMapper.deleteById(cid);
        return R.ok(null);
    }

    /* ================================================================== */
    /* Excel 批量导入                                                         */
    /* ================================================================== */

    @Operation(summary = "批量导入品牌（Excel）")
    @PostMapping("/import")
    @OperLog(module = "品牌管理", action = "批量导入", type = OperLog.OperType.CREATE)
    public R<Map<String, Object>> importBrands(
            @RequestParam("file") MultipartFile file) throws IOException {
        BrandImportListener listener = new BrandImportListener(brandService);
        EasyExcel.read(file.getInputStream(), BrandImportRow.class, listener).sheet().doRead();
        return R.ok(listener.getResult());
    }

    @Operation(summary = "下载品牌导入模板")
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=brand_import_template.xlsx");
        EasyExcel.write(response.getOutputStream(), BrandImportRow.class)
                .sheet("品牌导入模板")
                .doWrite(new ArrayList<>());
    }
}
