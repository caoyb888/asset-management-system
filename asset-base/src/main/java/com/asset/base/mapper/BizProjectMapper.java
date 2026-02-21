package com.asset.base.mapper;

import com.asset.base.model.dto.ProjectQuery;
import com.asset.base.model.vo.ProjectVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.asset.base.entity.BizProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 项目 Mapper
 * <p>BaseMapper 提供单表 CRUD；自定义方法走 XML 实现联表分页查询</p>
 */
@Mapper
public interface BizProjectMapper extends BaseMapper<BizProject> {

    /**
     * 分页查询项目列表（含公司名、负责人姓名）
     *
     * @param page  分页参数（MyBatis-Plus 自动处理 LIMIT）
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<ProjectVO> selectPageWithCond(
            @Param("page") Page<ProjectVO> page,
            @Param("q") ProjectQuery query);
}
