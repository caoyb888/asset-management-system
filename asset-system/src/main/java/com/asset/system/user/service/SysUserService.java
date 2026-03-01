package com.asset.system.user.service;

import com.asset.system.user.dto.ResetPwdDTO;
import com.asset.system.user.dto.UserCreateDTO;
import com.asset.system.user.dto.UserDetailVO;
import com.asset.system.user.dto.UserQueryDTO;
import com.asset.system.user.entity.SysUser;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/** 用户管理 Service */
public interface SysUserService extends IService<SysUser> {

    /** 分页查询用户列表 */
    IPage<UserDetailVO> pageQuery(UserQueryDTO query);

    /** 获取用户详情（含角色、岗位） */
    UserDetailVO getDetailById(Long id);

    /** 新增用户 */
    Long createUser(UserCreateDTO dto);

    /** 更新用户信息 */
    void updateUser(UserCreateDTO dto);

    /** 删除用户（逻辑删除，不可删超管） */
    void deleteUser(Long id);

    /** 重置密码 */
    void resetPassword(ResetPwdDTO dto);

    /** 修改用户状态（启用/停用） */
    void changeStatus(Long id, Integer status);
}
