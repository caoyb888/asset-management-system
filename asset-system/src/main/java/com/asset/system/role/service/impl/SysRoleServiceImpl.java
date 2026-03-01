package com.asset.system.role.service.impl;

import com.asset.system.common.exception.SysBizException;
import com.asset.system.common.exception.SysErrorCode;
import com.asset.system.role.dto.RoleCreateDTO;
import com.asset.system.role.dto.RoleDetailVO;
import com.asset.system.role.dto.RoleQueryDTO;
import com.asset.system.role.entity.SysRole;
import com.asset.system.role.entity.SysRoleMenu;
import com.asset.system.role.mapper.SysRoleMapper;
import com.asset.system.role.mapper.SysRoleMenuMapper;
import com.asset.system.role.service.SysRoleService;
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

/** 角色管理 ServiceImpl */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>
        implements SysRoleService {

    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public IPage<SysRole> pageQuery(RoleQueryDTO query) {
        return baseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysRole>()
                        .like(StringUtils.hasText(query.getRoleName()), SysRole::getRoleName, query.getRoleName())
                        .like(StringUtils.hasText(query.getRoleCode()), SysRole::getRoleCode, query.getRoleCode())
                        .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                        .orderByAsc(SysRole::getSortOrder));
    }

    @Override
    public RoleDetailVO getDetailById(Long id) {
        SysRole role = baseMapper.selectById(id);
        if (role == null) throw new SysBizException(SysErrorCode.ROLE_NOT_FOUND);
        RoleDetailVO vo = toDetailVO(role);
        vo.setMenuIds(roleMenuMapper.selectMenuIdsByRoleId(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(RoleCreateDTO dto) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, dto.getRoleCode()));
        if (count > 0) throw new SysBizException(SysErrorCode.ROLE_CODE_EXISTS);
        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        role.setDataScope(dto.getDataScope() != null ? dto.getDataScope() : 1);
        role.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        role.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        role.setRemark(dto.getRemark());
        baseMapper.insert(role);
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            saveRoleMenus(role.getId(), dto.getMenuIds());
        }
        log.info("[角色] 新增角色 {} id={}", dto.getRoleName(), role.getId());
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleCreateDTO dto) {
        SysRole exist = getOrThrow(dto.getId());
        if ("SUPER_ADMIN".equals(exist.getRoleCode())) throw new SysBizException(SysErrorCode.ROLE_IS_ADMIN);
        update(new LambdaUpdateWrapper<SysRole>()
                .eq(SysRole::getId, dto.getId())
                .set(StringUtils.hasText(dto.getRoleName()), SysRole::getRoleName, dto.getRoleName())
                .set(dto.getDataScope() != null, SysRole::getDataScope, dto.getDataScope())
                .set(dto.getSortOrder() != null, SysRole::getSortOrder, dto.getSortOrder())
                .set(dto.getStatus() != null, SysRole::getStatus, dto.getStatus())
                .set(dto.getRemark() != null, SysRole::setRemark, dto.getRemark()));
        if (dto.getMenuIds() != null) {
            roleMenuMapper.deleteByRoleId(dto.getId());
            if (!dto.getMenuIds().isEmpty()) saveRoleMenus(dto.getId(), dto.getMenuIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SysRole role = getOrThrow(id);
        if ("SUPER_ADMIN".equals(role.getRoleCode())) throw new SysBizException(SysErrorCode.ROLE_IS_ADMIN);
        removeById(id);
        roleMenuMapper.deleteByRoleId(id);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        getOrThrow(id);
        update(new LambdaUpdateWrapper<SysRole>().eq(SysRole::getId, id).set(SysRole::getStatus, status));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantMenus(Long roleId, List<Long> menuIds) {
        getOrThrow(roleId);
        roleMenuMapper.deleteByRoleId(roleId);
        if (menuIds != null && !menuIds.isEmpty()) saveRoleMenus(roleId, menuIds);
        log.info("[角色] 分配菜单 roleId={} menuCount={}", roleId, menuIds != null ? menuIds.size() : 0);
    }

    // ─── 私有辅助 ─────────────────────────────────────────────────────────────

    private SysRole getOrThrow(Long id) {
        SysRole role = baseMapper.selectById(id);
        if (role == null) throw new SysBizException(SysErrorCode.ROLE_NOT_FOUND);
        return role;
    }

    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        menuIds.forEach(menuId -> {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        });
    }

    private RoleDetailVO toDetailVO(SysRole role) {
        RoleDetailVO vo = new RoleDetailVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleCode(role.getRoleCode());
        vo.setDataScope(role.getDataScope());
        vo.setSortOrder(role.getSortOrder());
        vo.setStatus(role.getStatus());
        vo.setStatusName(role.getStatus() != null ? (role.getStatus() == 1 ? "正常" : "停用") : null);
        vo.setRemark(role.getRemark());
        vo.setCreatedAt(role.getCreatedAt());
        return vo;
    }
}
