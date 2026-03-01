package com.asset.system.user.service.impl;

import com.asset.common.exception.BizException;
import com.asset.system.common.exception.SysBizException;
import com.asset.system.common.exception.SysErrorCode;
import com.asset.system.role.entity.SysUserRole;
import com.asset.system.role.mapper.SysUserRoleMapper;
import com.asset.system.user.dto.ResetPwdDTO;
import com.asset.system.user.dto.UserCreateDTO;
import com.asset.system.user.dto.UserDetailVO;
import com.asset.system.user.dto.UserQueryDTO;
import com.asset.system.user.entity.SysUser;
import com.asset.system.user.mapper.SysUserMapper;
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

import java.util.List;

/**
 * 用户管理 ServiceImpl
 * <p>密码采用 SM3 哈希（实际由 asset-common-security 中的 SmCryptoUtil 处理）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
        implements SysUserService {

    private final SysUserRoleMapper userRoleMapper;

    @Override
    public IPage<UserDetailVO> pageQuery(UserQueryDTO query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getRealName()), SysUser::getRealName, query.getRealName())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .eq(query.getDeptId() != null, SysUser::getDeptId, query.getDeptId())
                .orderByDesc(SysUser::getId);

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
        // TODO: 实际应使用 SmCryptoUtil.sm3Hash(dto.getPassword()) 哈希密码
        user.setPassword(dto.getPassword());
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
        log.info("[用户] 删除用户 id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPwdDTO dto) {
        SysUser user = getOrThrow(dto.getUserId());
        // TODO: 实际使用 SmCryptoUtil.sm3Hash(dto.getNewPassword())
        update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, dto.getUserId())
                .set(SysUser::getPassword, dto.getNewPassword()));
        log.info("[用户] 重置密码 userId={}", dto.getUserId());
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        getOrThrow(id);
        update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getStatus, status));
        log.info("[用户] 修改状态 id={} status={}", id, status);
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
        return vo;
    }
}
