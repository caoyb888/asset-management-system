package com.asset.base.service.impl;

import com.asset.base.converter.ProjectConverter;
import com.asset.base.entity.BizProject;
import com.asset.base.entity.BizProjectBank;
import com.asset.base.entity.BizProjectContract;
import com.asset.base.entity.BizProjectFinanceContact;
import com.asset.base.mapper.BizProjectBankMapper;
import com.asset.base.mapper.BizProjectContractMapper;
import com.asset.base.mapper.BizProjectFinanceContactMapper;
import com.asset.base.mapper.BizProjectMapper;
import com.asset.base.model.dto.ProjectBankDTO;
import com.asset.base.model.dto.ProjectContractDTO;
import com.asset.base.model.dto.ProjectFinanceContactDTO;
import com.asset.base.model.dto.ProjectQuery;
import com.asset.base.model.dto.ProjectSaveDTO;
import com.asset.base.model.vo.ProjectBankVO;
import com.asset.base.model.vo.ProjectContractVO;
import com.asset.base.model.vo.ProjectFinanceContactVO;
import com.asset.base.model.vo.ProjectVO;
import com.asset.base.service.BizProjectService;
import com.asset.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目管理 Service 实现
 */
@Service
@RequiredArgsConstructor
public class BizProjectServiceImpl
        extends ServiceImpl<BizProjectMapper, BizProject>
        implements BizProjectService {

    private final ProjectConverter converter;
    private final BizProjectContractMapper contractMapper;
    private final BizProjectFinanceContactMapper financeContactMapper;
    private final BizProjectBankMapper bankMapper;

    // 产权性质
    private static final Map<Integer, String> PROPERTY_TYPE_MAP = Map.of(
            1, "国有", 2, "集体", 3, "私有", 4, "其他");

    // 经营类型
    private static final Map<Integer, String> BUSINESS_TYPE_MAP = Map.of(
            1, "自持", 2, "租赁", 3, "合作");

    // 运营状态
    private static final Map<Integer, String> OPERATION_STATUS_MAP = Map.of(
            0, "筹备", 1, "开业", 2, "停业");

    /* ------------------------------------------------------------------ */
    /* 查询                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    public IPage<ProjectVO> pageProject(ProjectQuery query) {
        Page<ProjectVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<ProjectVO> result = baseMapper.selectPageWithCond(page, query);
        // 填充枚举名称
        result.getRecords().forEach(this::fillEnumNames);
        return result;
    }

    @Override
    public ProjectVO getProjectById(Long id) {
        BizProject project = getById(id);
        if (project == null || project.getIsDeleted() == 1) {
            throw new BizException("项目不存在或已删除");
        }
        ProjectVO vo = converter.toVO(project);
        fillEnumNames(vo);
        return vo;
    }

    /* ------------------------------------------------------------------ */
    /* 新增                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProject(ProjectSaveDTO dto) {
        // 校验项目编号唯一性（未删除状态）
        checkProjectCodeUnique(dto.getProjectCode(), null);

        BizProject entity = converter.toEntity(dto);
        // 审计字段由 MetaObjectHandler 自动填充
        save(entity);
        return entity.getId();
    }

    /* ------------------------------------------------------------------ */
    /* 编辑                                                                  */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(Long id, ProjectSaveDTO dto) {
        BizProject existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("项目不存在或已删除");
        }

        // 若编号有变更则校验唯一性
        if (!existing.getProjectCode().equals(dto.getProjectCode())) {
            checkProjectCodeUnique(dto.getProjectCode(), id);
        }

        converter.updateEntity(dto, existing);
        updateById(existing);
    }

    /* ------------------------------------------------------------------ */
    /* 删除（逻辑删除）                                                       */
    /* ------------------------------------------------------------------ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id) {
        BizProject existing = getById(id);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("项目不存在或已删除");
        }
        // removeById 触发 @TableLogic，将 is_deleted 置为 1
        removeById(id);
    }

    /* ------------------------------------------------------------------ */
    /* 私有方法                                                               */
    /* ------------------------------------------------------------------ */

    /**
     * 校验项目编号在未删除状态下唯一
     *
     * @param code      待校验的编号
     * @param excludeId 编辑时排除自身ID
     */
    private void checkProjectCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<BizProject> wrapper = new LambdaQueryWrapper<BizProject>()
                .eq(BizProject::getProjectCode, code)
                .eq(BizProject::getIsDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(BizProject::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BizException("项目编号 [" + code + "] 已存在，请使用其他编号");
        }
    }

    /** 填充枚举名称字段 */
    private void fillEnumNames(ProjectVO vo) {
        if (vo.getPropertyType() != null) {
            vo.setPropertyTypeName(PROPERTY_TYPE_MAP.getOrDefault(vo.getPropertyType(), "未知"));
        }
        if (vo.getBusinessType() != null) {
            vo.setBusinessTypeName(BUSINESS_TYPE_MAP.getOrDefault(vo.getBusinessType(), "未知"));
        }
        if (vo.getOperationStatus() != null) {
            vo.setOperationStatusName(OPERATION_STATUS_MAP.getOrDefault(vo.getOperationStatus(), "未知"));
        }
    }

    /* ------------------------------------------------------------------ */
    /* 合同甲方信息                                                          */
    /* ------------------------------------------------------------------ */

    @Override
    public ProjectContractVO getContract(Long projectId) {
        BizProjectContract record = contractMapper.selectOne(
                new LambdaQueryWrapper<BizProjectContract>()
                        .eq(BizProjectContract::getProjectId, projectId)
                        .eq(BizProjectContract::getIsDeleted, 0)
                        .last("LIMIT 1")
        );
        if (record == null) {
            // 不存在时返回空对象，前端直接渲染表单
            ProjectContractVO empty = new ProjectContractVO();
            empty.setProjectId(projectId);
            return empty;
        }
        ProjectContractVO vo = new ProjectContractVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveContract(Long projectId, ProjectContractDTO dto) {
        BizProjectContract existing = contractMapper.selectOne(
                new LambdaQueryWrapper<BizProjectContract>()
                        .eq(BizProjectContract::getProjectId, projectId)
                        .eq(BizProjectContract::getIsDeleted, 0)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            // 更新已有记录
            BeanUtils.copyProperties(dto, existing, "id", "projectId");
            contractMapper.updateById(existing);
        } else {
            // 新增
            BizProjectContract record = new BizProjectContract();
            BeanUtils.copyProperties(dto, record);
            record.setProjectId(projectId);
            contractMapper.insert(record);
        }
    }

    /* ------------------------------------------------------------------ */
    /* 财务联系人                                                            */
    /* ------------------------------------------------------------------ */

    @Override
    public List<ProjectFinanceContactVO> listFinanceContacts(Long projectId) {
        List<BizProjectFinanceContact> list = financeContactMapper.selectList(
                new LambdaQueryWrapper<BizProjectFinanceContact>()
                        .eq(BizProjectFinanceContact::getProjectId, projectId)
                        .eq(BizProjectFinanceContact::getIsDeleted, 0)
                        .orderByAsc(BizProjectFinanceContact::getCreatedAt)
        );
        return list.stream().map(entity -> {
            ProjectFinanceContactVO vo = new ProjectFinanceContactVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFinanceContact(Long projectId, ProjectFinanceContactDTO dto) {
        BizProjectFinanceContact entity = new BizProjectFinanceContact();
        BeanUtils.copyProperties(dto, entity);
        entity.setProjectId(projectId);
        financeContactMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFinanceContact(Long contactId, ProjectFinanceContactDTO dto) {
        BizProjectFinanceContact existing = financeContactMapper.selectById(contactId);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("财务联系人不存在或已删除");
        }
        BeanUtils.copyProperties(dto, existing, "id", "projectId");
        financeContactMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFinanceContact(Long contactId) {
        BizProjectFinanceContact existing = financeContactMapper.selectById(contactId);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("财务联系人不存在或已删除");
        }
        financeContactMapper.deleteById(contactId);
    }

    /* ------------------------------------------------------------------ */
    /* 银行账号                                                              */
    /* ------------------------------------------------------------------ */

    @Override
    public List<ProjectBankVO> listBanks(Long projectId) {
        List<BizProjectBank> list = bankMapper.selectList(
                new LambdaQueryWrapper<BizProjectBank>()
                        .eq(BizProjectBank::getProjectId, projectId)
                        .eq(BizProjectBank::getIsDeleted, 0)
                        .orderByAsc(BizProjectBank::getCreatedAt)
        );
        return list.stream().map(entity -> {
            ProjectBankVO vo = new ProjectBankVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addBank(Long projectId, ProjectBankDTO dto) {
        BizProjectBank entity = new BizProjectBank();
        BeanUtils.copyProperties(dto, entity);
        entity.setProjectId(projectId);
        bankMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBank(Long bankId, ProjectBankDTO dto) {
        BizProjectBank existing = bankMapper.selectById(bankId);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("银行账号不存在或已删除");
        }
        BeanUtils.copyProperties(dto, existing, "id", "projectId");
        bankMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBank(Long bankId) {
        BizProjectBank existing = bankMapper.selectById(bankId);
        if (existing == null || existing.getIsDeleted() == 1) {
            throw new BizException("银行账号不存在或已删除");
        }
        bankMapper.deleteById(bankId);
    }
}
