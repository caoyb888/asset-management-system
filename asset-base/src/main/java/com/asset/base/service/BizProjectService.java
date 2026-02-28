package com.asset.base.service;

import com.asset.base.entity.BizProject;
import com.asset.base.model.dto.ProjectBankDTO;
import com.asset.base.model.dto.ProjectContractDTO;
import com.asset.base.model.dto.ProjectFinanceContactDTO;
import com.asset.base.model.dto.ProjectImageDTO;
import com.asset.base.model.dto.ProjectQuery;
import com.asset.base.model.dto.ProjectSaveDTO;
import com.asset.base.model.vo.ProjectBankVO;
import com.asset.base.model.vo.ProjectContractVO;
import com.asset.base.model.vo.ProjectFinanceContactVO;
import com.asset.base.model.vo.ProjectVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 项目管理 Service 接口
 */
public interface BizProjectService extends IService<BizProject> {

    /**
     * 分页查询项目列表
     *
     * @param query 查询条件（含分页参数）
     * @return 分页结果（含公司名、负责人姓名）
     */
    IPage<ProjectVO> pageProject(ProjectQuery query);

    /**
     * 查询项目详情
     *
     * @param id 项目ID
     * @return VO
     */
    ProjectVO getProjectById(Long id);

    /**
     * 新增项目
     * <p>校验同一项目编号在未删除状态下不可重复</p>
     *
     * @param dto 新增DTO
     * @return 新增后的项目ID
     */
    Long createProject(ProjectSaveDTO dto);

    /**
     * 编辑项目
     *
     * @param id  项目ID
     * @param dto 编辑DTO
     */
    void updateProject(Long id, ProjectSaveDTO dto);

    /**
     * 逻辑删除项目
     * <p>底层调用 MyBatis-Plus removeById，自动将 is_deleted 置为 1</p>
     *
     * @param id 项目ID
     */
    void deleteProject(Long id);

    /* ------------------------------------------------------------------ */
    /* 合同甲方信息                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * 获取项目合同甲方信息（不存在时返回空对象）
     *
     * @param projectId 项目ID
     * @return 合同甲方信息VO
     */
    ProjectContractVO getContract(Long projectId);

    /**
     * 保存项目合同甲方信息（存在则更新，不存在则新增）
     *
     * @param projectId 项目ID
     * @param dto       合同甲方信息DTO
     */
    void saveContract(Long projectId, ProjectContractDTO dto);

    /* ------------------------------------------------------------------ */
    /* 财务联系人                                                            */
    /* ------------------------------------------------------------------ */

    /**
     * 查询项目财务联系人列表（按创建时间升序）
     *
     * @param projectId 项目ID
     * @return 财务联系人列表
     */
    List<ProjectFinanceContactVO> listFinanceContacts(Long projectId);

    /**
     * 新增财务联系人
     *
     * @param projectId 项目ID
     * @param dto       财务联系人DTO
     * @return 新增记录ID
     */
    Long addFinanceContact(Long projectId, ProjectFinanceContactDTO dto);

    /**
     * 更新财务联系人
     *
     * @param contactId 联系人ID
     * @param dto       财务联系人DTO
     */
    void updateFinanceContact(Long contactId, ProjectFinanceContactDTO dto);

    /**
     * 删除财务联系人（校验归属项目）
     *
     * @param contactId 联系人ID
     */
    void deleteFinanceContact(Long contactId);

    /* ------------------------------------------------------------------ */
    /* 银行账号                                                              */
    /* ------------------------------------------------------------------ */

    /**
     * 查询项目银行账号列表
     *
     * @param projectId 项目ID
     * @return 银行账号列表
     */
    List<ProjectBankVO> listBanks(Long projectId);

    /**
     * 新增银行账号
     *
     * @param projectId 项目ID
     * @param dto       银行账号DTO
     * @return 新增记录ID
     */
    Long addBank(Long projectId, ProjectBankDTO dto);

    /**
     * 更新银行账号
     *
     * @param bankId 银行账号ID
     * @param dto    银行账号DTO
     */
    void updateBank(Long bankId, ProjectBankDTO dto);

    /**
     * 删除银行账号（校验归属项目）
     *
     * @param bankId 银行账号ID
     */
    void deleteBank(Long bankId);

    /* ------------------------------------------------------------------ */
    /* 项目图片                                                             */
    /* ------------------------------------------------------------------ */

    /**
     * 追加项目图片（写入 image_urls JSON 数组）
     *
     * @param id  项目ID
     * @param dto 图片信息（url + name）
     */
    void addProjectImage(Long id, ProjectImageDTO dto);

    /**
     * 删除项目图片（按下标从 image_urls 中移除）
     *
     * @param id    项目ID
     * @param index 图片下标（0起）
     */
    void deleteProjectImage(Long id, Integer index);
}
