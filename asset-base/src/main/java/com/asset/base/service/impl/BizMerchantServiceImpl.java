package com.asset.base.service.impl;

import com.asset.base.converter.MerchantConverter;
import com.asset.base.entity.BizMerchant;
import com.asset.base.entity.BizMerchantContact;
import com.asset.base.entity.BizMerchantInvoice;
import com.asset.base.mapper.BizMerchantContactMapper;
import com.asset.base.mapper.BizMerchantInvoiceMapper;
import com.asset.base.mapper.BizMerchantMapper;
import com.asset.base.model.dto.MerchantContactDTO;
import com.asset.base.model.dto.MerchantInvoiceDTO;
import com.asset.base.model.dto.MerchantQuery;
import com.asset.base.model.dto.MerchantSaveDTO;
import com.asset.base.model.vo.MerchantVO;
import com.asset.base.service.BizMerchantService;
import com.asset.common.exception.BizException;
import com.asset.common.security.crypto.SmCryptoUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 商家管理 Service 实现
 */
@Service
@RequiredArgsConstructor
public class BizMerchantServiceImpl
        extends ServiceImpl<BizMerchantMapper, BizMerchant>
        implements BizMerchantService {

    private final MerchantConverter converter;
    private final BizMerchantContactMapper contactMapper;
    private final BizMerchantInvoiceMapper invoiceMapper;

    private static final Map<Integer, String> MERCHANT_ATTR_MAP = Map.of(
            1, "个体户", 2, "企业");

    private static final Map<Integer, String> MERCHANT_NATURE_MAP = Map.of(
            1, "民营", 2, "国营", 3, "外资", 4, "合资");

    private static final Map<Integer, String> MERCHANT_LEVEL_MAP = Map.of(
            1, "优秀", 2, "良好", 3, "一般", 4, "差");

    private static final Map<Integer, String> AUDIT_STATUS_MAP = Map.of(
            0, "待审核", 1, "通过", 2, "驳回");

    /* ------------------------------------------------------------------ */
    /* 查询                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    public IPage<MerchantVO> pageMerchant(MerchantQuery query) {
        Page<MerchantVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<MerchantVO> result = baseMapper.selectPageWithCond(page, query);
        result.getRecords().forEach(vo -> {
            fillEnumNames(vo);
            // 身份证号脱敏（列表页仅显示脱敏值）
            vo.setIdCard(maskIdCard(vo.getIdCard()));
        });
        return result;
    }

    @Override
    public MerchantVO getMerchantById(Long id) {
        BizMerchant merchant = getById(id);
        if (merchant == null || merchant.getIsDeleted() == 1) {
            throw new BizException("商家不存在或已删除");
        }
        MerchantVO vo = converter.toVO(merchant);
        fillEnumNames(vo);
        // 解密身份证号（详情页返回明文，可按需再脱敏）
        if (StringUtils.hasText(vo.getIdCard())) {
            try {
                String plain = SmCryptoUtil.sm4Decrypt(vo.getIdCard());
                vo.setIdCard(maskIdCard(plain));
            } catch (Exception ignored) {
                // 旧数据可能未加密，直接脱敏
                vo.setIdCard(maskIdCard(vo.getIdCard()));
            }
        }
        // 查询联系人
        List<BizMerchantContact> contacts = contactMapper.selectList(
                new LambdaQueryWrapper<BizMerchantContact>()
                        .eq(BizMerchantContact::getMerchantId, id)
                        .eq(BizMerchantContact::getIsDeleted, 0)
                        .orderByDesc(BizMerchantContact::getIsPrimary)
        );
        vo.setContacts(contacts.stream().map(converter::toContactVO).toList());
        // 查询开票信息
        List<BizMerchantInvoice> invoices = invoiceMapper.selectList(
                new LambdaQueryWrapper<BizMerchantInvoice>()
                        .eq(BizMerchantInvoice::getMerchantId, id)
                        .eq(BizMerchantInvoice::getIsDeleted, 0)
                        .orderByDesc(BizMerchantInvoice::getIsDefault)
        );
        vo.setInvoices(invoices.stream().map(converter::toInvoiceVO).toList());
        return vo;
    }

    /* ------------------------------------------------------------------ */
    /* 新增                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMerchant(MerchantSaveDTO dto) {
        BizMerchant entity = converter.toEntity(dto);
        // 身份证号 SM4 加密
        if (StringUtils.hasText(entity.getIdCard())) {
            entity.setIdCard(SmCryptoUtil.sm4Encrypt(entity.getIdCard()));
        }
        save(entity);
        saveContacts(entity.getId(), dto.getContacts());
        saveInvoices(entity.getId(), dto.getInvoices());
        return entity.getId();
    }

    /* ------------------------------------------------------------------ */
    /* 编辑                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMerchant(Long id, MerchantSaveDTO dto) {
        BizMerchant existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("商家不存在或已删除");
        }
        converter.updateEntity(dto, existing);
        // 身份证号 SM4 加密
        if (StringUtils.hasText(dto.getIdCard())) {
            existing.setIdCard(SmCryptoUtil.sm4Encrypt(dto.getIdCard()));
        }
        updateById(existing);
        // 重建联系人和开票信息
        contactMapper.delete(new LambdaQueryWrapper<BizMerchantContact>()
                .eq(BizMerchantContact::getMerchantId, id));
        invoiceMapper.delete(new LambdaQueryWrapper<BizMerchantInvoice>()
                .eq(BizMerchantInvoice::getMerchantId, id));
        saveContacts(id, dto.getContacts());
        saveInvoices(id, dto.getInvoices());
    }

    /* ------------------------------------------------------------------ */
    /* 删除                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMerchant(Long id) {
        BizMerchant existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("商家不存在或已删除");
        }
        removeById(id);
    }

    /* ------------------------------------------------------------------ */
    /* 私有方法                                                               */
    /* ------------------------------------------------------------------ */

    private void saveContacts(Long merchantId, List<MerchantContactDTO> dtos) {
        if (CollectionUtils.isEmpty(dtos)) {
            return;
        }
        dtos.forEach(dto -> {
            BizMerchantContact contact = converter.toContactEntity(dto);
            contact.setId(null); // 重建联系人时清除旧id，由雪花算法重新生成
            contact.setMerchantId(merchantId);
            contactMapper.insert(contact);
        });
    }

    private void saveInvoices(Long merchantId, List<MerchantInvoiceDTO> dtos) {
        if (CollectionUtils.isEmpty(dtos)) {
            return;
        }
        dtos.forEach(dto -> {
            BizMerchantInvoice invoice = converter.toInvoiceEntity(dto);
            invoice.setId(null); // 重建开票信息时清除旧id，由雪花算法重新生成
            invoice.setMerchantId(merchantId);
            invoiceMapper.insert(invoice);
        });
    }

    /** 身份证号脱敏（保留前6后4位） */
    private String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    private void fillEnumNames(MerchantVO vo) {
        if (vo.getMerchantAttr() != null) {
            vo.setMerchantAttrName(MERCHANT_ATTR_MAP.getOrDefault(vo.getMerchantAttr(), "未知"));
        }
        if (vo.getMerchantNature() != null) {
            vo.setMerchantNatureName(MERCHANT_NATURE_MAP.getOrDefault(vo.getMerchantNature(), "未知"));
        }
        if (vo.getMerchantLevel() != null) {
            vo.setMerchantLevelName(MERCHANT_LEVEL_MAP.getOrDefault(vo.getMerchantLevel(), "未知"));
        }
        if (vo.getAuditStatus() != null) {
            vo.setAuditStatusName(AUDIT_STATUS_MAP.getOrDefault(vo.getAuditStatus(), "未知"));
        }
    }
}
