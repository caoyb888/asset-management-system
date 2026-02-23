package com.asset.base.controller;

import com.alibaba.excel.EasyExcel;
import com.asset.base.entity.BizMerchant;
import com.asset.base.entity.BizMerchantAttachment;
import com.asset.base.entity.BizMerchantContact;
import com.asset.base.entity.BizMerchantCredit;
import com.asset.base.entity.BizMerchantInvoice;
import com.asset.base.excel.MerchantImportListener;
import com.asset.base.mapper.BizMerchantAttachmentMapper;
import com.asset.base.mapper.BizMerchantContactMapper;
import com.asset.base.mapper.BizMerchantCreditMapper;
import com.asset.base.mapper.BizMerchantInvoiceMapper;
import com.asset.base.model.dto.AttachmentSaveDTO;
import com.asset.base.model.dto.CreditSaveDTO;
import com.asset.base.model.dto.InvoiceSaveDTO;
import com.asset.base.model.dto.MerchantContactDTO;
import com.asset.base.model.dto.MerchantImportRow;
import com.asset.base.model.dto.MerchantQuery;
import com.asset.base.model.dto.MerchantSaveDTO;
import com.asset.base.model.vo.AttachmentVO;
import com.asset.base.model.vo.CreditVO;
import com.asset.base.model.vo.MerchantContactVO;
import com.asset.base.model.vo.MerchantInvoiceVO;
import com.asset.base.model.vo.MerchantVO;
import com.asset.base.service.BizMerchantService;
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
 * 商家管理 Controller
 *
 * <pre>
 * GET    /base/merchants                         分页查询
 * POST   /base/merchants                         新增商家
 * GET    /base/merchants/{id}                    商家详情
 * PUT    /base/merchants/{id}                    编辑商家
 * DELETE /base/merchants/{id}                    逻辑删除
 * PUT    /base/merchants/{id}/audit              审核
 * GET    /base/merchants/{id}/contacts           联系人列表
 * POST   /base/merchants/{id}/contacts           新增联系人
 * PUT    /base/merchants/{id}/contacts/{cid}     编辑联系人
 * DELETE /base/merchants/{id}/contacts/{cid}     删除联系人
 * GET    /base/merchants/{id}/credits            诚信记录列表
 * POST   /base/merchants/{id}/credits            新增诚信记录
 * DELETE /base/merchants/{id}/credits/{rid}      删除诚信记录
 * GET    /base/merchants/{id}/invoices           开票信息列表
 * POST   /base/merchants/{id}/invoices           新增开票信息
 * PUT    /base/merchants/{id}/invoices/{iid}     编辑开票信息
 * DELETE /base/merchants/{id}/invoices/{iid}     删除开票信息
 * GET    /base/merchants/{id}/attachments        附件列表
 * POST   /base/merchants/{id}/attachments        上传附件记录
 * DELETE /base/merchants/{id}/attachments/{aid}  删除附件
 * POST   /base/merchants/import                  批量导入
 * GET    /base/merchants/template                下载导入模板
 * </pre>
 */
@Tag(name = "商家管理", description = "基础数据-商家增删改查")
@RestController
@RequestMapping("/base/merchants")
@RequiredArgsConstructor
public class BizMerchantController {

    private final BizMerchantService merchantService;
    private final BizMerchantContactMapper contactMapper;
    private final BizMerchantInvoiceMapper invoiceMapper;
    private final BizMerchantCreditMapper creditMapper;
    private final BizMerchantAttachmentMapper attachmentMapper;

    /** 诚信记录类型映射 */
    private static final Map<Integer, String> RECORD_TYPE_MAP = Map.of(
            1, "好评", 2, "差评", 3, "违约", 4, "其他");

    /* ================================================================== */
    /* 商家基础 CRUD                                                          */
    /* ================================================================== */

    @Operation(summary = "分页查询商家列表")
    @GetMapping
    @OperLog(module = "商家管理", action = "分页查询", type = OperLog.OperType.QUERY)
    public R<IPage<MerchantVO>> page(MerchantQuery query) {
        return R.ok(merchantService.pageMerchant(query));
    }

    @Operation(summary = "查询商家详情")
    @GetMapping("/{id}")
    @OperLog(module = "商家管理", action = "查询详情", type = OperLog.OperType.QUERY)
    public R<MerchantVO> detail(
            @Parameter(description = "商家ID") @PathVariable Long id) {
        return R.ok(merchantService.getMerchantById(id));
    }

    @Operation(summary = "新增商家")
    @PostMapping
    @OperLog(module = "商家管理", action = "新增", type = OperLog.OperType.CREATE)
    public R<Long> create(@Valid @RequestBody MerchantSaveDTO dto) {
        return R.ok(merchantService.createMerchant(dto));
    }

    @Operation(summary = "编辑商家")
    @PutMapping("/{id}")
    @OperLog(module = "商家管理", action = "编辑", type = OperLog.OperType.UPDATE)
    public R<Void> update(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Valid @RequestBody MerchantSaveDTO dto) {
        merchantService.updateMerchant(id, dto);
        return R.ok(null);
    }

    @Operation(summary = "删除商家（逻辑删除）")
    @DeleteMapping("/{id}")
    @OperLog(module = "商家管理", action = "删除", type = OperLog.OperType.DELETE)
    public R<Void> delete(
            @Parameter(description = "商家ID") @PathVariable Long id) {
        merchantService.deleteMerchant(id);
        return R.ok(null);
    }

    /* ================================================================== */
    /* 商家审核                                                               */
    /* ================================================================== */

    @Operation(summary = "商家审核（1-通过，2-驳回）")
    @PutMapping("/{id}/audit")
    @OperLog(module = "商家管理", action = "审核", type = OperLog.OperType.UPDATE)
    public R<Void> audit(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Integer auditStatus = (Integer) body.get("auditStatus");
        if (auditStatus == null || (auditStatus != 1 && auditStatus != 2)) {
            throw new BizException("审核状态只能为1(通过)或2(驳回)");
        }
        BizMerchant merchant = merchantService.getById(id);
        if (merchant == null || merchant.getIsDeleted() == 1) {
            throw new BizException("商家不存在");
        }
        merchant.setAuditStatus(auditStatus);
        merchantService.updateById(merchant);
        return R.ok(null);
    }

    /* ================================================================== */
    /* 商家联系人独立 CRUD                                                    */
    /* ================================================================== */

    @Operation(summary = "查询商家联系人列表")
    @GetMapping("/{id}/contacts")
    @OperLog(module = "商家管理", action = "查询联系人", type = OperLog.OperType.QUERY)
    public R<List<MerchantContactVO>> listContacts(
            @Parameter(description = "商家ID") @PathVariable Long id) {
        checkMerchantExists(id);
        List<BizMerchantContact> list = contactMapper.selectList(
                new LambdaQueryWrapper<BizMerchantContact>()
                        .eq(BizMerchantContact::getMerchantId, id)
                        .eq(BizMerchantContact::getIsDeleted, 0)
                        .orderByDesc(BizMerchantContact::getIsPrimary)
                        .orderByAsc(BizMerchantContact::getCreatedAt)
        );
        List<MerchantContactVO> voList = list.stream().map(c -> {
            MerchantContactVO vo = new MerchantContactVO();
            vo.setId(c.getId());
            vo.setMerchantId(c.getMerchantId());
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

    @Operation(summary = "新增商家联系人")
    @PostMapping("/{id}/contacts")
    @OperLog(module = "商家管理", action = "新增联系人", type = OperLog.OperType.CREATE)
    public R<Long> addContact(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Valid @RequestBody MerchantContactDTO dto) {
        checkMerchantExists(id);
        BizMerchantContact contact = new BizMerchantContact();
        contact.setMerchantId(id);
        contact.setContactName(dto.getContactName());
        contact.setPhone(dto.getPhone());
        contact.setEmail(dto.getEmail());
        contact.setPosition(dto.getPosition());
        contact.setIsPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : 0);
        contactMapper.insert(contact);
        return R.ok(contact.getId());
    }

    @Operation(summary = "编辑商家联系人")
    @PutMapping("/{id}/contacts/{cid}")
    @OperLog(module = "商家管理", action = "编辑联系人", type = OperLog.OperType.UPDATE)
    public R<Void> updateContact(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Parameter(description = "联系人ID") @PathVariable Long cid,
            @Valid @RequestBody MerchantContactDTO dto) {
        BizMerchantContact contact = contactMapper.selectById(cid);
        if (contact == null || contact.getIsDeleted() == 1) {
            throw new BizException("联系人不存在或已删除");
        }
        if (!id.equals(contact.getMerchantId())) {
            throw new BizException("联系人不属于该商家");
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

    @Operation(summary = "删除商家联系人（逻辑删除）")
    @DeleteMapping("/{id}/contacts/{cid}")
    @OperLog(module = "商家管理", action = "删除联系人", type = OperLog.OperType.DELETE)
    public R<Void> deleteContact(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Parameter(description = "联系人ID") @PathVariable Long cid) {
        BizMerchantContact contact = contactMapper.selectById(cid);
        if (contact == null || contact.getIsDeleted() == 1) {
            throw new BizException("联系人不存在或已删除");
        }
        if (!id.equals(contact.getMerchantId())) {
            throw new BizException("联系人不属于该商家");
        }
        contactMapper.deleteById(cid);
        return R.ok(null);
    }

    /* ================================================================== */
    /* 商家诚信记录                                                            */
    /* ================================================================== */

    @Operation(summary = "查询商家诚信记录列表")
    @GetMapping("/{id}/credits")
    @OperLog(module = "商家管理", action = "查询诚信记录", type = OperLog.OperType.QUERY)
    public R<List<CreditVO>> listCredits(
            @Parameter(description = "商家ID") @PathVariable Long id) {
        checkMerchantExists(id);
        List<BizMerchantCredit> list = creditMapper.selectList(
                new LambdaQueryWrapper<BizMerchantCredit>()
                        .eq(BizMerchantCredit::getMerchantId, id)
                        .eq(BizMerchantCredit::getIsDeleted, 0)
                        .orderByDesc(BizMerchantCredit::getCreatedAt)
        );
        List<CreditVO> voList = list.stream().map(c -> {
            CreditVO vo = new CreditVO();
            vo.setId(c.getId());
            vo.setMerchantId(c.getMerchantId());
            vo.setRecordType(c.getRecordType());
            vo.setRecordTypeName(RECORD_TYPE_MAP.getOrDefault(c.getRecordType(), "其他"));
            vo.setContent(c.getContent());
            vo.setRecordDate(c.getRecordDate());
            vo.setOperatorId(c.getOperatorId());
            vo.setAttachmentUrl(c.getAttachmentUrl());
            vo.setCreatedAt(c.getCreatedAt());
            return vo;
        }).toList();
        return R.ok(voList);
    }

    @Operation(summary = "新增商家诚信记录")
    @PostMapping("/{id}/credits")
    @OperLog(module = "商家管理", action = "新增诚信记录", type = OperLog.OperType.CREATE)
    public R<Long> addCredit(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Valid @RequestBody CreditSaveDTO dto) {
        checkMerchantExists(id);
        BizMerchantCredit credit = new BizMerchantCredit();
        credit.setMerchantId(id);
        credit.setRecordType(dto.getRecordType());
        credit.setContent(dto.getContent());
        credit.setRecordDate(dto.getRecordDate());
        credit.setAttachmentUrl(dto.getAttachmentUrl());
        creditMapper.insert(credit);
        return R.ok(credit.getId());
    }

    @Operation(summary = "删除商家诚信记录（逻辑删除）")
    @DeleteMapping("/{id}/credits/{rid}")
    @OperLog(module = "商家管理", action = "删除诚信记录", type = OperLog.OperType.DELETE)
    public R<Void> deleteCredit(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Parameter(description = "诚信记录ID") @PathVariable Long rid) {
        BizMerchantCredit credit = creditMapper.selectById(rid);
        if (credit == null || credit.getIsDeleted() == 1) {
            throw new BizException("诚信记录不存在或已删除");
        }
        if (!id.equals(credit.getMerchantId())) {
            throw new BizException("诚信记录不属于该商家");
        }
        creditMapper.deleteById(rid);
        return R.ok(null);
    }

    /* ================================================================== */
    /* 商家开票信息独立 CRUD                                                   */
    /* ================================================================== */

    @Operation(summary = "查询商家开票信息列表")
    @GetMapping("/{id}/invoices")
    @OperLog(module = "商家管理", action = "查询开票信息", type = OperLog.OperType.QUERY)
    public R<List<MerchantInvoiceVO>> listInvoices(
            @Parameter(description = "商家ID") @PathVariable Long id) {
        checkMerchantExists(id);
        List<BizMerchantInvoice> list = invoiceMapper.selectList(
                new LambdaQueryWrapper<BizMerchantInvoice>()
                        .eq(BizMerchantInvoice::getMerchantId, id)
                        .eq(BizMerchantInvoice::getIsDeleted, 0)
                        .orderByDesc(BizMerchantInvoice::getIsDefault)
        );
        List<MerchantInvoiceVO> voList = list.stream().map(inv -> {
            MerchantInvoiceVO vo = new MerchantInvoiceVO();
            vo.setId(inv.getId());
            vo.setMerchantId(inv.getMerchantId());
            vo.setInvoiceTitle(inv.getInvoiceTitle());
            vo.setTaxNumber(inv.getTaxNumber());
            vo.setBankName(inv.getBankName());
            vo.setBankAccount(inv.getBankAccount());
            vo.setAddress(inv.getAddress());
            vo.setPhone(inv.getPhone());
            vo.setIsDefault(inv.getIsDefault());
            return vo;
        }).toList();
        return R.ok(voList);
    }

    @Operation(summary = "新增商家开票信息")
    @PostMapping("/{id}/invoices")
    @OperLog(module = "商家管理", action = "新增开票信息", type = OperLog.OperType.CREATE)
    public R<Long> addInvoice(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @RequestBody InvoiceSaveDTO dto) {
        checkMerchantExists(id);
        BizMerchantInvoice invoice = new BizMerchantInvoice();
        invoice.setMerchantId(id);
        invoice.setInvoiceTitle(dto.getInvoiceTitle());
        invoice.setTaxNumber(dto.getTaxNumber());
        invoice.setBankName(dto.getBankName());
        invoice.setBankAccount(dto.getBankAccount());
        invoice.setAddress(dto.getAddress());
        invoice.setPhone(dto.getPhone());
        invoice.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : 0);
        invoiceMapper.insert(invoice);
        return R.ok(invoice.getId());
    }

    @Operation(summary = "编辑商家开票信息")
    @PutMapping("/{id}/invoices/{iid}")
    @OperLog(module = "商家管理", action = "编辑开票信息", type = OperLog.OperType.UPDATE)
    public R<Void> updateInvoice(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Parameter(description = "开票信息ID") @PathVariable Long iid,
            @RequestBody InvoiceSaveDTO dto) {
        BizMerchantInvoice invoice = invoiceMapper.selectById(iid);
        if (invoice == null || invoice.getIsDeleted() == 1) {
            throw new BizException("开票信息不存在或已删除");
        }
        if (!id.equals(invoice.getMerchantId())) {
            throw new BizException("开票信息不属于该商家");
        }
        invoice.setInvoiceTitle(dto.getInvoiceTitle());
        invoice.setTaxNumber(dto.getTaxNumber());
        invoice.setBankName(dto.getBankName());
        invoice.setBankAccount(dto.getBankAccount());
        invoice.setAddress(dto.getAddress());
        invoice.setPhone(dto.getPhone());
        if (dto.getIsDefault() != null) {
            invoice.setIsDefault(dto.getIsDefault());
        }
        invoiceMapper.updateById(invoice);
        return R.ok(null);
    }

    @Operation(summary = "删除商家开票信息（逻辑删除）")
    @DeleteMapping("/{id}/invoices/{iid}")
    @OperLog(module = "商家管理", action = "删除开票信息", type = OperLog.OperType.DELETE)
    public R<Void> deleteInvoice(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Parameter(description = "开票信息ID") @PathVariable Long iid) {
        BizMerchantInvoice invoice = invoiceMapper.selectById(iid);
        if (invoice == null || invoice.getIsDeleted() == 1) {
            throw new BizException("开票信息不存在或已删除");
        }
        if (!id.equals(invoice.getMerchantId())) {
            throw new BizException("开票信息不属于该商家");
        }
        invoiceMapper.deleteById(iid);
        return R.ok(null);
    }

    /* ================================================================== */
    /* 商家附件                                                               */
    /* ================================================================== */

    @Operation(summary = "查询商家附件列表")
    @GetMapping("/{id}/attachments")
    @OperLog(module = "商家管理", action = "查询附件", type = OperLog.OperType.QUERY)
    public R<List<AttachmentVO>> listAttachments(
            @Parameter(description = "商家ID") @PathVariable Long id) {
        checkMerchantExists(id);
        List<BizMerchantAttachment> list = attachmentMapper.selectList(
                new LambdaQueryWrapper<BizMerchantAttachment>()
                        .eq(BizMerchantAttachment::getMerchantId, id)
                        .eq(BizMerchantAttachment::getIsDeleted, 0)
                        .orderByDesc(BizMerchantAttachment::getCreatedAt)
        );
        List<AttachmentVO> voList = list.stream().map(a -> {
            AttachmentVO vo = new AttachmentVO();
            vo.setId(a.getId());
            vo.setMerchantId(a.getMerchantId());
            vo.setFileName(a.getFileName());
            vo.setFileUrl(a.getFileUrl());
            vo.setFileType(a.getFileType());
            vo.setFileSize(a.getFileSize());
            vo.setUploadBy(a.getUploadBy());
            vo.setCreatedAt(a.getCreatedAt());
            return vo;
        }).toList();
        return R.ok(voList);
    }

    @Operation(summary = "保存商家附件记录（文件已由 file 服务上传，此处只保存元数据）")
    @PostMapping("/{id}/attachments")
    @OperLog(module = "商家管理", action = "上传附件", type = OperLog.OperType.CREATE)
    public R<Long> addAttachment(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Valid @RequestBody AttachmentSaveDTO dto) {
        checkMerchantExists(id);
        BizMerchantAttachment attachment = new BizMerchantAttachment();
        attachment.setMerchantId(id);
        attachment.setFileName(dto.getFileName());
        attachment.setFileUrl(dto.getFileUrl());
        attachment.setFileType(dto.getFileType());
        attachment.setFileSize(dto.getFileSize());
        attachmentMapper.insert(attachment);
        return R.ok(attachment.getId());
    }

    @Operation(summary = "删除商家附件（逻辑删除）")
    @DeleteMapping("/{id}/attachments/{aid}")
    @OperLog(module = "商家管理", action = "删除附件", type = OperLog.OperType.DELETE)
    public R<Void> deleteAttachment(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Parameter(description = "附件ID") @PathVariable Long aid) {
        BizMerchantAttachment attachment = attachmentMapper.selectById(aid);
        if (attachment == null || attachment.getIsDeleted() == 1) {
            throw new BizException("附件不存在或已删除");
        }
        if (!id.equals(attachment.getMerchantId())) {
            throw new BizException("附件不属于该商家");
        }
        attachmentMapper.deleteById(aid);
        return R.ok(null);
    }

    /* ================================================================== */
    /* Excel 批量导入                                                         */
    /* ================================================================== */

    @Operation(summary = "批量导入商家（Excel）")
    @PostMapping("/import")
    @OperLog(module = "商家管理", action = "批量导入", type = OperLog.OperType.CREATE)
    public R<Map<String, Object>> importMerchants(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Long projectId) throws IOException {
        MerchantImportListener listener = new MerchantImportListener(merchantService, projectId);
        EasyExcel.read(file.getInputStream(), MerchantImportRow.class, listener).sheet().doRead();
        return R.ok(listener.getResult());
    }

    @Operation(summary = "下载商家导入模板")
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=merchant_import_template.xlsx");
        EasyExcel.write(response.getOutputStream(), MerchantImportRow.class)
                .sheet("商家导入模板")
                .doWrite(new ArrayList<>());
    }

    /* ================================================================== */
    /* 私有辅助方法                                                            */
    /* ================================================================== */

    /** 校验商家存在，不存在则抛出 BizException */
    private void checkMerchantExists(Long id) {
        BizMerchant merchant = merchantService.getById(id);
        if (merchant == null || merchant.getIsDeleted() == 1) {
            throw new BizException("商家不存在或已删除");
        }
    }
}
