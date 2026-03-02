package com.asset.report.mapper.perm;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 报表数据权限查询 Mapper
 * <p>
 * 跨模块读取系统权限表（sys_user_role、sys_role）和业务数据表（biz_project），
 * 用于解析当前用户可见的项目 ID 列表。
 * </p>
 * <p>注：报表服务为只读型服务，此处仅做查询，无写操作。</p>
 */
@Mapper
public interface ReportPermMapper {

    /**
     * 判断指定用户是否拥有超级管理员角色（data_scope=1，即全部数据权限）
     * 或 username='admin'。
     *
     * @param userId 用户ID
     * @return true=管理员，false=普通用户
     */
    boolean isAdminUser(@Param("userId") Long userId);

    /**
     * 查询所有未删除的项目 ID 列表（管理员调用）
     */
    List<Long> selectAllProjectIds();

    /**
     * 查询指定用户作为负责人的项目 ID 列表（biz_project.manager_id = userId）
     *
     * @param userId 用户ID
     */
    List<Long> selectProjectIdsByManagerId(@Param("userId") Long userId);

    /**
     * 查询指定用户通过自定义数据权限（dataScope=2）可访问的部门所关联的项目 ID 列表
     * （sys_role_data → dept_id → biz_project.dept_id，若 biz_project 有 dept_id 字段时使用）
     *
     * @param userId 用户ID
     */
    List<Long> selectProjectIdsByUserDeptScope(@Param("userId") Long userId);
}
