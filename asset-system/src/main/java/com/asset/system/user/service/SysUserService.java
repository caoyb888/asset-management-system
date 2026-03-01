package com.asset.system.user.service;

import com.asset.system.user.dto.ChangePasswordDTO;
import com.asset.system.user.dto.ResetPwdDTO;
import com.asset.system.user.dto.UserCreateDTO;
import com.asset.system.user.dto.UserDetailVO;
import com.asset.system.user.dto.UserProfileDTO;
import com.asset.system.user.dto.UserQueryDTO;
import com.asset.system.user.entity.SysUser;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** 用户管理 Service */
public interface SysUserService extends IService<SysUser> {

    /** 分页查询用户列表 */
    IPage<UserDetailVO> pageQuery(UserQueryDTO query);

    /** 获取用户详情（含角色、岗位、部门名） */
    UserDetailVO getDetailById(Long id);

    /** 新增用户 */
    Long createUser(UserCreateDTO dto);

    /** 更新用户信息 */
    void updateUser(UserCreateDTO dto);

    /** 删除用户（逻辑删除，不可删超管） */
    void deleteUser(Long id);

    /** 重置密码（管理员操作） */
    void resetPassword(ResetPwdDTO dto);

    /** 修改用户状态（启用/停用） */
    void changeStatus(Long id, Integer status);

    /** 修改个人资料（当前登录用户） */
    void updateProfile(Long userId, UserProfileDTO dto);

    /** 修改自身密码（需校验原密码，密码为 SM3 明文，非 SM2 密文） */
    void changePassword(Long userId, ChangePasswordDTO dto);

    /** 分配角色（全量替换） */
    void assignRoles(Long userId, List<Long> roleIds);

    /** 分配岗位（全量替换） */
    void assignPosts(Long userId, List<Long> postIds);

    /** 强制下线（清除 Redis 中的 refresh token） */
    void forceOffline(Long userId);
}
