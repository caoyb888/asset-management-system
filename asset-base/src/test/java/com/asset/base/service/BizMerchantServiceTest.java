package com.asset.base.service;

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
import com.asset.base.model.vo.MerchantContactVO;
import com.asset.base.model.vo.MerchantInvoiceVO;
import com.asset.base.model.vo.MerchantVO;
import com.asset.base.service.impl.BizMerchantServiceImpl;
import com.asset.common.exception.BizException;
import com.asset.common.security.crypto.SmCryptoUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 商家管理 Service 单元测试（MCH-U-01 ~ MCH-U-10）
 * 含 SM4 加解密 static 方法 mock（需 mockito-inline 依赖）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("商家管理 Service 单元测试")
class BizMerchantServiceTest {

    @Mock
    BizMerchantMapper merchantMapper;

    @Mock
    MerchantConverter converter;

    @Mock
    BizMerchantContactMapper contactMapper;

    @Mock
    BizMerchantInvoiceMapper invoiceMapper;

    @Spy
    @InjectMocks
    BizMerchantServiceImpl merchantService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(merchantService, "baseMapper", merchantMapper);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-U-01 新增-身份证号 SM4 加密
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-U-01 新增商家-身份证号SM4加密入库")
    void createMerchant_idCardEncrypted() {
        MerchantSaveDTO dto = new MerchantSaveDTO();
        dto.setProjectId(1L);
        dto.setMerchantName("测试商家");
        dto.setIdCard("110101199001011234");

        BizMerchant entity = new BizMerchant();
        entity.setId(100L);
        entity.setIdCard("110101199001011234"); // 明文

        when(converter.toEntity(dto)).thenReturn(entity);
        doReturn(true).when(merchantService).save(entity);

        try (MockedStatic<SmCryptoUtil> mocked = mockStatic(SmCryptoUtil.class)) {
            mocked.when(() -> SmCryptoUtil.sm4Encrypt("110101199001011234"))
                    .thenReturn("SM4_CIPHER_TEXT");

            merchantService.createMerchant(dto);

            assertThat(entity.getIdCard()).isEqualTo("SM4_CIPHER_TEXT");
            mocked.verify(() -> SmCryptoUtil.sm4Encrypt("110101199001011234"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-U-02 新增-无身份证跳过加密
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-U-02 新增商家-身份证为null-不调用sm4Encrypt")
    void createMerchant_nullIdCard_skipEncrypt() {
        MerchantSaveDTO dto = new MerchantSaveDTO();
        dto.setProjectId(1L);
        dto.setMerchantName("无身份证商家");
        dto.setIdCard(null);

        BizMerchant entity = new BizMerchant();
        entity.setId(101L);
        entity.setIdCard(null);

        when(converter.toEntity(dto)).thenReturn(entity);
        doReturn(true).when(merchantService).save(entity);

        try (MockedStatic<SmCryptoUtil> mocked = mockStatic(SmCryptoUtil.class)) {
            merchantService.createMerchant(dto);

            mocked.verify(() -> SmCryptoUtil.sm4Encrypt(any()), never());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-U-03 编辑-身份证重新加密
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-U-03 编辑商家-传入新身份证-调用sm4Encrypt")
    void updateMerchant_idCardReEncrypted() {
        BizMerchant existing = new BizMerchant();
        existing.setId(1L);
        existing.setIsDeleted(0);

        MerchantSaveDTO dto = new MerchantSaveDTO();
        dto.setProjectId(1L);
        dto.setMerchantName("商家");
        dto.setIdCard("440106199505051234");

        doReturn(existing).when(merchantService).getById(1L);
        doReturn(true).when(merchantService).updateById(any());
        when(contactMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(invoiceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        try (MockedStatic<SmCryptoUtil> mocked = mockStatic(SmCryptoUtil.class)) {
            mocked.when(() -> SmCryptoUtil.sm4Encrypt("440106199505051234"))
                    .thenReturn("NEW_CIPHER");

            merchantService.updateMerchant(1L, dto);

            mocked.verify(() -> SmCryptoUtil.sm4Encrypt("440106199505051234"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-U-04 列表-身份证脱敏格式（前6后4）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-U-04 列表查询-身份证已脱敏为前6后4格式")
    void pageMerchant_idCardMasked() {
        MerchantVO vo = new MerchantVO();
        vo.setIdCard("110101199001011234"); // DB 中明文

        Page<MerchantVO> mockPage = new Page<>();
        mockPage.setRecords(List.of(vo));

        when(merchantMapper.selectPageWithCond(any(), any())).thenReturn(mockPage);

        MerchantQuery query = new MerchantQuery();
        query.setPageNum(1);
        query.setPageSize(10);

        IPage<MerchantVO> result = merchantService.pageMerchant(query);

        assertThat(result.getRecords().get(0).getIdCard()).isEqualTo("110101********1234");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-U-05 详情-身份证解密后脱敏
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-U-05 商家详情-身份证解密后脱敏")
    void getMerchantById_idCardDecryptedAndMasked() {
        BizMerchant merchant = new BizMerchant();
        merchant.setId(1L);
        merchant.setIsDeleted(0);

        MerchantVO vo = new MerchantVO();
        vo.setIdCard("SM4_CIPHER"); // DB 中密文

        doReturn(merchant).when(merchantService).getById(1L);
        when(converter.toVO(merchant)).thenReturn(vo);
        when(contactMapper.selectList(any())).thenReturn(List.of());
        when(invoiceMapper.selectList(any())).thenReturn(List.of());

        try (MockedStatic<SmCryptoUtil> mocked = mockStatic(SmCryptoUtil.class)) {
            mocked.when(() -> SmCryptoUtil.sm4Decrypt("SM4_CIPHER"))
                    .thenReturn("110101199001011234");

            MerchantVO result = merchantService.getMerchantById(1L);

            assertThat(result.getIdCard()).isEqualTo("110101********1234");
            mocked.verify(() -> SmCryptoUtil.sm4Decrypt("SM4_CIPHER"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-U-06 详情-旧数据明文直接脱敏（解密异常时捕获）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-U-06 商家详情-解密异常时直接对明文脱敏")
    void getMerchantById_plainIdCard_maskedDirectly() {
        BizMerchant merchant = new BizMerchant();
        merchant.setId(1L);
        merchant.setIsDeleted(0);

        MerchantVO vo = new MerchantVO();
        vo.setIdCard("110101199001011234"); // 明文（旧数据）

        doReturn(merchant).when(merchantService).getById(1L);
        when(converter.toVO(merchant)).thenReturn(vo);
        when(contactMapper.selectList(any())).thenReturn(List.of());
        when(invoiceMapper.selectList(any())).thenReturn(List.of());

        try (MockedStatic<SmCryptoUtil> mocked = mockStatic(SmCryptoUtil.class)) {
            // 解密抛出异常，模拟旧数据未加密
            mocked.when(() -> SmCryptoUtil.sm4Decrypt("110101199001011234"))
                    .thenThrow(new RuntimeException("解密失败"));

            MerchantVO result = merchantService.getMerchantById(1L);

            // 直接对明文脱敏
            assertThat(result.getIdCard()).isEqualTo("110101********1234");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-U-07 详情-联系人和开票信息均返回
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-U-07 商家详情-联系人+开票信息均返回")
    void getMerchantById_includesContactsAndInvoices() {
        BizMerchant merchant = new BizMerchant();
        merchant.setId(1L);
        merchant.setIsDeleted(0);

        MerchantVO vo = new MerchantVO();
        vo.setIdCard(null); // 无身份证

        BizMerchantContact contact = new BizMerchantContact();
        BizMerchantInvoice invoice = new BizMerchantInvoice();

        doReturn(merchant).when(merchantService).getById(1L);
        when(converter.toVO(merchant)).thenReturn(vo);
        when(contactMapper.selectList(any())).thenReturn(List.of(contact));
        when(invoiceMapper.selectList(any())).thenReturn(List.of(invoice));
        when(converter.toContactVO(contact)).thenReturn(new MerchantContactVO());
        when(converter.toInvoiceVO(invoice)).thenReturn(new MerchantInvoiceVO());

        MerchantVO result = merchantService.getMerchantById(1L);

        assertThat(result.getContacts()).hasSize(1);
        assertThat(result.getInvoices()).hasSize(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-U-08 编辑-重建联系人和开票信息
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-U-08 编辑商家-重建联系人和开票信息-各先delete再insert 2次")
    void updateMerchant_rebuildsContactsAndInvoices() {
        BizMerchant existing = new BizMerchant();
        existing.setId(1L);
        existing.setIsDeleted(0);

        MerchantContactDTO c1 = new MerchantContactDTO();
        c1.setContactName("新联系人A");
        MerchantContactDTO c2 = new MerchantContactDTO();
        c2.setContactName("新联系人B");

        MerchantInvoiceDTO inv1 = new MerchantInvoiceDTO();
        inv1.setInvoiceTitle("发票抬头A");
        MerchantInvoiceDTO inv2 = new MerchantInvoiceDTO();
        inv2.setInvoiceTitle("发票抬头B");

        MerchantSaveDTO dto = new MerchantSaveDTO();
        dto.setProjectId(1L);
        dto.setMerchantName("更新商家");
        dto.setContacts(List.of(c1, c2));
        dto.setInvoices(List.of(inv1, inv2));

        doReturn(existing).when(merchantService).getById(1L);
        doReturn(true).when(merchantService).updateById(any());
        when(contactMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(invoiceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(converter.toContactEntity(any(MerchantContactDTO.class)))
                .thenAnswer(inv -> new BizMerchantContact());
        when(converter.toInvoiceEntity(any(MerchantInvoiceDTO.class)))
                .thenAnswer(inv -> new BizMerchantInvoice());
        when(contactMapper.insert(any(BizMerchantContact.class))).thenReturn(1);
        when(invoiceMapper.insert(any(BizMerchantInvoice.class))).thenReturn(1);

        merchantService.updateMerchant(1L, dto);

        verify(contactMapper).delete(any(LambdaQueryWrapper.class));
        verify(invoiceMapper).delete(any(LambdaQueryWrapper.class));
        verify(contactMapper, times(2)).insert(any(BizMerchantContact.class));
        verify(invoiceMapper, times(2)).insert(any(BizMerchantInvoice.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-U-09 删除-不存在商家
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-U-09 删除商家-ID不存在-抛出BizException")
    void deleteMerchant_notFound_throws() {
        doReturn(null).when(merchantService).getById(999999L);

        assertThatThrownBy(() -> merchantService.deleteMerchant(999999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MCH-U-10 脱敏规则-短字符串不脱敏（≤9位）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MCH-U-10 脱敏规则-身份证短于10位-原样返回")
    void pageMerchant_shortIdCard_returnRaw() {
        MerchantVO vo = new MerchantVO();
        vo.setIdCard("123"); // 短字符串

        Page<MerchantVO> mockPage = new Page<>();
        mockPage.setRecords(List.of(vo));

        when(merchantMapper.selectPageWithCond(any(), any())).thenReturn(mockPage);

        MerchantQuery query = new MerchantQuery();
        query.setPageNum(1);
        query.setPageSize(10);

        IPage<MerchantVO> result = merchantService.pageMerchant(query);

        // 短字符串直接原样返回
        assertThat(result.getRecords().get(0).getIdCard()).isEqualTo("123");
    }
}
