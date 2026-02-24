package com.asset.investment.contract.service;

import com.asset.investment.contract.dto.*;
import com.asset.investment.contract.entity.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 招商合同 Service
 * 涵盖任务 5.1（意向转合同+分布式锁）、5.2（合同CRUD+状态机）、5.3（版本管理）
 */
public interface InvLeaseContractService extends IService<InvLeaseContract> {

    /**
     * 多条件分页查询（仅查 isCurrent=1 的当前版本）
     */
    IPage<InvLeaseContract> pageQuery(ContractQueryDTO query);

    // ================================================================
    // 任务 5.1 — 意向转合同（分布式锁）
    // ================================================================

    /**
     * 意向协议转换为租赁合同
     * 前提：意向状态必须为"审批通过"(status=2)
     * 1. 对每个关联商铺获取 Redisson 分布式锁（3s 等待，30s TTL）
     * 2. 双重校验：商铺无审批中/生效的合同
     * 3. 复制意向字段 + dto.contractType/contractName 创建合同主记录
     * 4. 批量复制商铺/费项/费项阶段
     * 5. 更新意向状态为 CONVERTED(4)
     * 6. 写版本快照
     *
     * @return 新合同ID
     */
    Long convertFromIntention(Long intentionId, ContractSaveDTO dto);

    // ================================================================
    // 任务 5.2 — 合同CRUD与状态机
    // ================================================================

    /**
     * 新增合同（草稿状态，自动生成编号）
     */
    Long createContract(ContractSaveDTO dto);

    /**
     * 编辑合同
     * 仅草稿(0)状态可修改
     */
    void updateContract(Long id, ContractSaveDTO dto);

    /**
     * 删除合同
     * 审批中(1)和生效(2)状态不可删除
     */
    void deleteContract(Long id);

    /**
     * 发起审批
     * 草稿(0) → 审批中(1)
     */
    void submitApproval(Long id);

    /**
     * 审批回调
     * 审批中(1) → 生效(2) 或 草稿(0)（驳回回到草稿）
     */
    void handleApprovalCallback(Long id, ContractApprovalCallbackDTO dto);

    /**
     * 更新合同状态（到期/终止等系统操作）
     */
    void updateStatus(Long id, Integer status);

    // ================================================================
    // 商铺关联、费项配置、分铺计租
    // ================================================================

    /**
     * 批量保存商铺关联（全量替换）
     * 仅草稿状态可操作
     */
    void saveShops(Long id, List<ContractShopItemDTO> shops);

    /**
     * 查询合同关联商铺列表
     */
    List<InvLeaseContractShop> listShops(Long id);

    /**
     * 批量保存费项配置（全量替换，含 formula_params）
     * 仅草稿状态可操作
     */
    void saveFees(Long id, List<ContractFeeItemDTO> fees);

    /**
     * 查询费项列表
     */
    List<InvLeaseContractFee> listFees(Long id);

    /**
     * 批量保存分铺计租阶段（按费项分组替换）
     * 仅草稿状态可操作
     */
    void saveFeeStages(Long id, List<ContractFeeStageItemDTO> stages);

    /**
     * 查询全部分铺计租阶段
     */
    List<InvLeaseContractFeeStage> listFeeStages(Long id);

    /**
     * 调用计算引擎，生成费用明细并更新 total_amount
     */
    Map<String, Object> generateCost(Long id);

    /**
     * 调用账期生成器，生成账期列表并持久化（全量替换）
     */
    List<InvLeaseContractBilling> generateBilling(Long id);

    /**
     * 查询账期列表
     */
    List<InvLeaseContractBilling> listBilling(Long id);

    // ================================================================
    // 任务 5.3 — 版本管理
    // ================================================================

    /**
     * 创建合同版本快照
     * 将合同主体序列化为 JSON 写入 inv_lease_contract_version
     */
    void createSnapshot(Long contractId, String changeReason);

    /**
     * 查询合同版本历史列表（按版本号降序）
     */
    List<InvLeaseContractVersion> listVersions(Long contractId);
}
