package com.asset.finance.receipt;

import com.asset.finance.FinanceTestBase;
import com.asset.finance.receipt.dto.ReceiptCreateDTO;
import com.asset.finance.receipt.dto.ReceiptDetailItemDTO;
import com.asset.finance.receipt.dto.ReceiptDetailVO;
import com.asset.finance.receipt.dto.ReceiptQueryDTO;
import com.asset.finance.receipt.entity.FinReceipt;
import com.asset.finance.receipt.entity.FinReceiptDetail;
import com.asset.finance.receipt.mapper.FinReceiptDetailMapper;
import com.asset.finance.receipt.mapper.FinReceiptMapper;
import com.asset.finance.receipt.service.FinReceiptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * 收款管理 Service 单元测试
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>RCT-01：新增收款单-拆分合计等于总额</li>
 *   <li>RCT-02：新增收款单-拆分合计不等于总额 → FIN_4008</li>
 *   <li>RCT-03：查询收款详情-含拆分明细</li>
 *   <li>RCT-04：编辑收款单-仅待核销可编辑</li>
 *   <li>RCT-05：编辑收款单-部分核销不可编辑</li>
 *   <li>RCT-06：作废收款单-待核销可作废</li>
 *   <li>RCT-07：作废收款单-已核销不可作废</li>
 *   <li>RCT-08：未名款绑定-归名成功</li>
 *   <li>RCT-09：分页查询-按未名款筛选</li>
 *   <li>RCT-10：新增收款单-生成唯一编号</li>
 * </ol>
 */
@DisplayName("收款管理 Service 测试")
class FinReceiptServiceTest extends FinanceTestBase {

    @Autowired
    private FinReceiptService receiptService;

    @Autowired
    private FinReceiptMapper receiptMapper;

    @Autowired
    private FinReceiptDetailMapper detailMapper;

    // ─── 辅助方法 ────────────────────────────────────────────────────────────────

    /** 构建基础收款单 DTO */
    private ReceiptCreateDTO buildCreateDTO(BigDecimal totalAmount, List<ReceiptDetailItemDTO> details) {
        ReceiptCreateDTO dto = new ReceiptCreateDTO();
        dto.setTotalAmount(totalAmount);
        dto.setReceiptDate(LocalDate.now());
        dto.setPaymentMethod(1);
        dto.setPayerName("测试付款方");
        dto.setBankSerialNo("BANK-" + System.nanoTime());
        dto.setDetails(details);
        return dto;
    }

    /** 构建拆分明细项 */
    private ReceiptDetailItemDTO buildDetailItem(String feeName, BigDecimal amount) {
        ReceiptDetailItemDTO item = new ReceiptDetailItemDTO();
        item.setFeeName(feeName);
        item.setAmount(amount);
        return item;
    }

    /** 直接插入一条指定状态的收款单（绕过 service 校验） */
    private FinReceipt insertReceipt(int status, int isUnnamed) {
        FinReceipt r = new FinReceipt();
        r.setReceiptCode("RC-TEST-" + System.nanoTime());
        r.setContractId(10001L);
        r.setTotalAmount(new BigDecimal("1000.00"));
        r.setPaymentMethod(1);
        r.setReceiptDate(LocalDate.now());
        r.setStatus(status);
        r.setIsUnnamed(isUnnamed);
        r.setWriteOffAmount(BigDecimal.ZERO);
        r.setPrepayAmount(BigDecimal.ZERO);
        receiptMapper.insert(r);
        return r;
    }

    // ─── RCT-01：新增收款单-拆分合计等于总额 ──────────────────────────────────────

    @Test
    @DisplayName("RCT-01：新增收款单-拆分合计等于总额，保存成功")
    void create_withMatchingDetails_shouldSucceed() {
        List<ReceiptDetailItemDTO> details = Arrays.asList(
                buildDetailItem("租金", new BigDecimal("600.00")),
                buildDetailItem("物业费", new BigDecimal("400.00"))
        );
        ReceiptCreateDTO dto = buildCreateDTO(new BigDecimal("1000.00"), details);

        Long id = receiptService.create(dto);

        assertThat(id).as("新增应返回非空ID").isNotNull();

        FinReceipt saved = receiptMapper.selectById(id);
        assertThat(saved.getReceiptCode())
                .as("编号应以 RC- 开头")
                .startsWith("RC-");
        assertThat(saved.getStatus())
                .as("新建收款单状态应为0(待核销)")
                .isEqualTo(0);
        assertThat(saved.getTotalAmount())
                .as("总金额应为1000")
                .isEqualByComparingTo("1000.00");

        // 验证拆分明细
        List<FinReceiptDetail> savedDetails = detailMapper.selectList(
                new LambdaQueryWrapper<FinReceiptDetail>().eq(FinReceiptDetail::getReceiptId, id));
        assertThat(savedDetails).as("应有2条拆分明细").hasSize(2);
    }

    // ─── RCT-02：新增收款单-拆分合计不等于总额 ────────────────────────────────────

    @Test
    @DisplayName("RCT-02：新增收款单-拆分合计不等于总额 → 抛出异常")
    void create_withMismatchedDetails_shouldThrow() {
        List<ReceiptDetailItemDTO> details = Arrays.asList(
                buildDetailItem("租金", new BigDecimal("600.00")),
                buildDetailItem("物业费", new BigDecimal("300.00"))  // 合计900 ≠ 1000
        );
        ReceiptCreateDTO dto = buildCreateDTO(new BigDecimal("1000.00"), details);

        assertThatThrownBy(() -> receiptService.create(dto))
                .as("拆分合计900不等于总额1000应抛出异常")
                .isInstanceOf(Exception.class);
    }

    // ─── RCT-03：查询收款详情-含拆分明细 ──────────────────────────────────────────

    @Test
    @DisplayName("RCT-03：查询收款详情应包含拆分明细列表")
    void getDetailById_shouldContainDetails() {
        // 先创建含明细的收款单
        List<ReceiptDetailItemDTO> details = Arrays.asList(
                buildDetailItem("租金", new BigDecimal("700.00")),
                buildDetailItem("物业费", new BigDecimal("300.00"))
        );
        ReceiptCreateDTO dto = buildCreateDTO(new BigDecimal("1000.00"), details);
        Long id = receiptService.create(dto);

        ReceiptDetailVO vo = receiptService.getDetailById(id);

        assertThat(vo).as("详情VO不应为空").isNotNull();
        assertThat(vo.getReceiptCode()).as("编号应以RC-开头").startsWith("RC-");
        assertThat(vo.getDetails())
                .as("详情应包含2条拆分明细")
                .hasSize(2);
    }

    // ─── RCT-04：编辑收款单-仅待核销可编辑 ────────────────────────────────────────

    @Test
    @DisplayName("RCT-04：编辑待核销(status=0)的收款单成功")
    void update_statusZero_shouldSucceed() {
        FinReceipt receipt = insertReceipt(0, 0);

        ReceiptCreateDTO dto = buildCreateDTO(new BigDecimal("2000.00"), null);
        dto.setPayerName("更新后付款方");

        receiptService.update(receipt.getId(), dto);

        FinReceipt updated = receiptMapper.selectById(receipt.getId());
        assertThat(updated.getTotalAmount())
                .as("总金额应更新为2000")
                .isEqualByComparingTo("2000.00");
        assertThat(updated.getPayerName())
                .as("付款方应更新")
                .isEqualTo("更新后付款方");
    }

    // ─── RCT-05：编辑收款单-部分核销不可编辑 ──────────────────────────────────────

    @Test
    @DisplayName("RCT-05：编辑部分核销(status=1)的收款单应抛出异常")
    void update_statusOne_shouldThrow() {
        FinReceipt receipt = insertReceipt(1, 0);

        ReceiptCreateDTO dto = buildCreateDTO(new BigDecimal("2000.00"), null);

        assertThatThrownBy(() -> receiptService.update(receipt.getId(), dto))
                .as("status=1 不可编辑，应抛出异常")
                .isInstanceOf(Exception.class);
    }

    // ─── RCT-06：作废收款单-待核销可作废 ──────────────────────────────────────────

    @Test
    @DisplayName("RCT-06：作废待核销(status=0)的收款单成功")
    void cancel_statusZero_shouldSucceed() {
        FinReceipt receipt = insertReceipt(0, 0);

        receiptService.cancel(receipt.getId(), "录入错误");

        FinReceipt updated = receiptMapper.selectById(receipt.getId());
        assertThat(updated.getStatus())
                .as("作废后状态应为3(已作废)")
                .isEqualTo(3);
    }

    // ─── RCT-07：作废收款单-已核销不可作废 ────────────────────────────────────────

    @Test
    @DisplayName("RCT-07：作废已全部核销(status=2)的收款单应抛出异常")
    void cancel_statusTwo_shouldThrow() {
        FinReceipt receipt = insertReceipt(2, 0);

        assertThatThrownBy(() -> receiptService.cancel(receipt.getId(), "误操作"))
                .as("status=2 不可作废，应抛出异常")
                .isInstanceOf(Exception.class);
    }

    // ─── RCT-08：未名款绑定-归名成功 ──────────────────────────────────────────────

    @Test
    @DisplayName("RCT-08：未名款绑定合同后 is_unnamed=0，contract_id 已更新")
    void bind_unnamedReceipt_shouldUpdateFields() {
        FinReceipt receipt = insertReceipt(0, 1); // is_unnamed=1
        Long contractId = 50001L;

        receiptService.bind(receipt.getId(), contractId);

        FinReceipt updated = receiptMapper.selectById(receipt.getId());
        assertThat(updated.getIsUnnamed())
                .as("归名后 is_unnamed 应为0")
                .isEqualTo(0);
        assertThat(updated.getContractId())
                .as("归名后 contract_id 应已更新")
                .isEqualTo(contractId);
    }

    // ─── RCT-09：分页查询-按未名款筛选 ────────────────────────────────────────────

    @Test
    @DisplayName("RCT-09：分页查询按 isUnnamed=1 筛选仅返回未名款")
    void pageQuery_byUnnamed_shouldFilterCorrectly() {
        // 插入1条正常收款单 + 1条未名款
        insertReceipt(0, 0); // 正常
        insertReceipt(0, 1); // 未名款

        ReceiptQueryDTO query = new ReceiptQueryDTO();
        query.setIsUnnamed(1);
        query.setPageNum(1);
        query.setPageSize(50);

        IPage<ReceiptDetailVO> page = receiptService.pageQuery(query);

        assertThat(page.getRecords())
                .as("按未名款筛选应仅返回 is_unnamed=1 的记录")
                .isNotEmpty()
                .allSatisfy(vo -> assertThat(vo.getIsUnnamed()).isEqualTo(1));
    }

    // ─── RCT-10：新增收款单-生成唯一编号 ──────────────────────────────────────────

    @Test
    @DisplayName("RCT-10：连续新增3笔收款单，编号互不相同")
    void create_threeTimes_shouldGenerateUniqueCode() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            ReceiptCreateDTO dto = buildCreateDTO(new BigDecimal("100.00"), null);
            Long id = receiptService.create(dto);
            FinReceipt saved = receiptMapper.selectById(id);
            codes.add(saved.getReceiptCode());
        }

        assertThat(codes)
                .as("3次创建应产生3个不同的编号")
                .hasSize(3);
    }
}
