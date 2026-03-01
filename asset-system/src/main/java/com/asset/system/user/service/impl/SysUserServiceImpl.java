package com.asset.system.user.service.impl;

import com.asset.common.security.crypto.SmCryptoUtil;
import com.asset.system.auth.service.SysTokenService;
import com.asset.system.common.exception.SysBizException;
import com.asset.system.common.exception.SysErrorCode;
import com.asset.system.dept.entity.SysDept;
import com.asset.system.dept.mapper.SysDeptMapper;
import com.asset.system.post.entity.SysPost;
import com.asset.system.role.entity.SysRole;
import com.asset.system.role.entity.SysUserRole;
import com.asset.system.role.mapper.SysRoleMapper;
import com.asset.system.role.mapper.SysUserRoleMapper;
import com.asset.system.user.dto.ChangePasswordDTO;
import com.asset.system.user.dto.ResetPwdDTO;
import com.asset.system.user.dto.UserCreateDTO;
import com.asset.system.user.dto.UserDetailVO;
import com.asset.system.user.dto.UserProfileDTO;
import com.asset.system.user.dto.UserQueryDTO;
import com.asset.system.user.entity.SysUser;
import com.asset.system.user.entity.SysUserPost;
import com.asset.system.user.mapper.SysUserMapper;
import com.asset.system.user.mapper.SysUserPostMapper;
import com.asset.system.user.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.asset.system.common.datascope.DataScopeContext;
import com.asset.system.common.datascope.DataScopeInfo;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理 ServiceImpl
 * <p>密码采用 SM3 哈希存储；管理员重置密码时直接对明文哈希后存储。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
        implements SysUserService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysRoleMapper    roleMapper;
    private final SysDeptMapper    deptMapper;
    private final SysTokenService  tokenService;

    @Override
    public IPage<UserDetailVO> pageQuery(UserQueryDTO query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getRealName()), SysUser::getRealName, query.getRealName())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .eq(query.getDeptId() != null, SysUser::getDeptId, query.getDeptId())
                .orderByDesc(SysUser::getId);

        // ─── 数据权限过滤 ───────────────────────────────────────────────────────
        DataScopeInfo scope = DataScopeContext.get();
        if (scope != null && !scope.isAdmin()) {
            if (scope.isSelfOnly()) {
                // 仅本人：过滤 created_by = userId
                wrapper.eq(SysUser::getCreatedBy, scope.getUserId());
            } else if (scope.getDeptIds() != null) {
                if (scope.getDeptIds().isEmpty()) {
                    // 无任何部门权限：返回空
                    wrapper.apply("1 = 0");
                } else {
                    // 限定部门范围（前端传入 deptId 再交集）
                    if (query.getDeptId() == null) {
                        wrapper.in(SysUser::getDeptId, scope.getDeptIds());
                    } else {
                        // 前端筛了具体部门，若不在权限范围内则强制无结果
                        if (!scope.getDeptIds().contains(query.getDeptId())) {
                            wrapper.apply("1 = 0");
                        }
                    }
                }
            }
        }
        // ────────────────────────────────────────────────────────────────────────

        IPage<SysUser> page = baseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        return page.convert(this::toDetailVO);
    }

    @Override
    public UserDetailVO getDetailById(Long id) {
        SysUser user = baseMapper.selectById(id);
        if (user == null) throw new SysBizException(SysErrorCode.USER_NOT_FOUND);
        return toDetailVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateDTO dto) {
        // 用户名唯一性校验
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) throw new SysBizException(SysErrorCode.USER_ALREADY_EXISTS);

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        // SM3 哈希存储密码
        user.setPassword(StringUtils.hasText(dto.getPassword())
                ? SmCryptoUtil.sm3Hash(dto.getPassword()) : SmCryptoUtil.sm3Hash("123456"));
        user.setRealName(dto.getRealName());
        user.setDeptId(dto.getDeptId());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        baseMapper.insert(user);

        // 绑定角色
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            saveUserRoles(user.getId(), dto.getRoleIds());
        }

        // 绑定岗位
        if (dto.getPostIds() != null && !dto.getPostIds().isEmpty()) {
            saveUserPosts(user.getId(), dto.getPostIds());
        }

        log.info("[用户] 新增用户 {} id={}", dto.getUsername(), user.getId());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserCreateDTO dto) {
        SysUser exist = getOrThrow(dto.getId());
        if ("SUPER_ADMIN".equals(exist.getUsername())) {
            throw new SysBizException(SysErrorCode.USER_HAS_ADMIN_ROLE);
        }
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, dto.getId())
                .set(StringUtils.hasText(dto.getRealName()), SysUser::getRealName, dto.getRealName())
                .set(dto.getDeptId() != null, SysUser::getDeptId, dto.getDeptId())
                .set(StringUtils.hasText(dto.getPhone()), SysUser::getPhone, dto.getPhone())
                .set(StringUtils.hasText(dto.getEmail()), SysUser::getEmail, dto.getEmail())
                .set(StringUtils.hasText(dto.getAvatar()), SysUser::getAvatar, dto.getAvatar())
                .set(dto.getStatus() != null, SysUser::getStatus, dto.getStatus());
        update(wrapper);

        // 更新角色
        if (dto.getRoleIds() != null) {
            userRoleMapper.deleteByUserId(dto.getId());
            if (!dto.getRoleIds().isEmpty()) {
                saveUserRoles(dto.getId(), dto.getRoleIds());
            }
        }

        // 更新岗位
        if (dto.getPostIds() != null) {
            userPostMapper.deleteByUserId(dto.getId());
            if (!dto.getPostIds().isEmpty()) {
                saveUserPosts(dto.getId(), dto.getPostIds());
            }
        }

        log.info("[用户] 更新用户 id={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysUser user = getOrThrow(id);
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new SysBizException(SysErrorCode.USER_DELETE_SELF_FORBIDDEN);
        }
        removeById(id);
        userRoleMapper.deleteByUserId(id);
        userPostMapper.deleteByUserId(id);
        log.info("[用户] 删除用户 id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPwdDTO dto) {
        getOrThrow(dto.getUserId());
        update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, dto.getUserId())
                .set(SysUser::getPassword, SmCryptoUtil.sm3Hash(dto.getNewPassword())));
        log.info("[用户] 管理员重置密码 userId={}", dto.getUserId());
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        getOrThrow(id);
        update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getStatus, status));
        log.info("[用户] 修改状态 id={} status={}", id, status);
    }

    @Override
    public void updateProfile(Long userId, UserProfileDTO dto) {
        getOrThrow(userId);
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(StringUtils.hasText(dto.getRealName()), SysUser::getRealName, dto.getRealName())
                .set(StringUtils.hasText(dto.getPhone()), SysUser::getPhone, dto.getPhone())
                .set(StringUtils.hasText(dto.getEmail()), SysUser::getEmail, dto.getEmail())
                .set(StringUtils.hasText(dto.getAvatar()), SysUser::getAvatar, dto.getAvatar());
        update(wrapper);
        log.info("[用户] 更新个人资料 userId={}", userId);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        // 通过 selectByIdWithPwd 获取含密码字段的用户
        SysUser user = baseMapper.selectByIdWithPwd(userId);
        if (user == null) throw new SysBizException(SysErrorCode.USER_NOT_FOUND);
        // 校验原密码
        if (!SmCryptoUtil.sm3Matches(dto.getOldPassword(), user.getPassword())) {
            throw new SysBizException(SysErrorCode.USER_OLD_PWD_WRONG);
        }
        update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getPassword, SmCryptoUtil.sm3Hash(dto.getNewPassword())));
        log.info("[用户] 修改自身密码 userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        getOrThrow(userId);
        userRoleMapper.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            saveUserRoles(userId, roleIds);
        }
        log.info("[用户] 分配角色 userId={} roles={}", userId, roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPosts(Long userId, List<Long> postIds) {
        getOrThrow(userId);
        userPostMapper.deleteByUserId(userId);
        if (postIds != null && !postIds.isEmpty()) {
            saveUserPosts(userId, postIds);
        }
        log.info("[用户] 分配岗位 userId={} posts={}", userId, postIds);
    }

    @Override
    public void forceOffline(Long userId) {
        getOrThrow(userId);
        tokenService.removeAllRefreshTokensByUser(userId);
        log.info("[用户] 强制下线 userId={}", userId);
    }

    // ─── 私有辅助 ─────────────────────────────────────────────────────────────

    private SysUser getOrThrow(Long id) {
        SysUser user = baseMapper.selectById(id);
        if (user == null) throw new SysBizException(SysErrorCode.USER_NOT_FOUND);
        return user;
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        roleIds.forEach(roleId -> {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        });
    }

    private void saveUserPosts(Long userId, List<Long> postIds) {
        postIds.forEach(postId -> {
            SysUserPost up = new SysUserPost();
            up.setUserId(userId);
            up.setPostId(postId);
            userPostMapper.insert(up);
        });
    }

    private UserDetailVO toDetailVO(SysUser user) {
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setDeptId(user.getDeptId());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setStatusName(user.getStatus() != null ? (user.getStatus() == 1 ? "正常" : "停用") : null);
        vo.setLoginIp(user.getLoginIp());
        vo.setLoginTime(user.getLoginTime());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());

        // 加载部门名称
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) vo.setDeptName(dept.getDeptName());
        }

        // 加载角色信息
        List<SysRole> roles = roleMapper.selectByUserId(user.getId());
        if (roles != null && !roles.isEmpty()) {
            vo.setRoleIds(roles.stream().map(SysRole::getId).collect(Collectors.toList()));
            vo.setRoleNames(roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));
        } else {
            vo.setRoleIds(Collections.emptyList());
            vo.setRoleNames(Collections.emptyList());
        }

        // 加载岗位信息
        List<SysPost> posts = userPostMapper.selectPostsByUserId(user.getId());
        if (posts != null && !posts.isEmpty()) {
            vo.setPostIds(posts.stream().map(SysPost::getId).collect(Collectors.toList()));
            vo.setPostNames(posts.stream().map(SysPost::getPostName).collect(Collectors.toList()));
        } else {
            vo.setPostIds(Collections.emptyList());
            vo.setPostNames(Collections.emptyList());
        }

        return vo;
    }
}
