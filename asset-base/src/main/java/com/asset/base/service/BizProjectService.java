package com.asset.base.service;

import com.asset.base.entity.BizProject;
import com.asset.base.model.dto.ProjectQuery;
import com.asset.base.model.dto.ProjectSaveDTO;
import com.asset.base.model.vo.ProjectVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

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
}
