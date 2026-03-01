package com.asset.system.menu.service.impl;

import com.asset.system.common.exception.SysBizException;
import com.asset.system.common.exception.SysErrorCode;
import com.asset.system.menu.dto.MenuCreateDTO;
import com.asset.system.menu.dto.MenuTreeVO;
import com.asset.system.menu.entity.SysMenu;
import com.asset.system.menu.mapper.SysMenuMapper;
import com.asset.system.menu.service.SysMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 菜单管理 ServiceImpl */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu>
        implements SysMenuService {

    @Override
    public List<MenuTreeVO> getMenuTree() {
        List<SysMenu> all = baseMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getParentId, SysMenu::getSortOrder));
        return buildTree(all, 0L);
    }

    @Override
    public List<MenuTreeVO> getRouteTree(Long userId) {
        List<SysMenu> menus = baseMapper.selectRoutesByUserId(userId);
        return buildTree(menus, 0L);
    }

    @Override
    public List<String> getPermsByUserId(Long userId) {
        return baseMapper.selectPermsByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMenu(MenuCreateDTO dto) {
        SysMenu menu = new SysMenu();
        menu.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        menu.setMenuName(dto.getMenuName());
        menu.setMenuType(dto.getMenuType());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setPerms(dto.getPerms());
        menu.setIcon(dto.getIcon());
        menu.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        menu.setVisible(dto.getVisible() != null ? dto.getVisible() : 1);
        menu.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        menu.setRemark(dto.getRemark());
        baseMapper.insert(menu);
        log.info("[菜单] 新增菜单 {} id={}", menu.getMenuName(), menu.getId());
        return menu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(MenuCreateDTO dto) {
        if (baseMapper.selectById(dto.getId()) == null) throw new SysBizException(SysErrorCode.MENU_NOT_FOUND);
        SysMenu menu = new SysMenu();
        menu.setId(dto.getId());
        menu.setParentId(dto.getParentId());
        menu.setMenuName(dto.getMenuName());
        menu.setMenuType(dto.getMenuType());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setPerms(dto.getPerms());
        menu.setIcon(dto.getIcon());
        menu.setSortOrder(dto.getSortOrder());
        menu.setVisible(dto.getVisible());
        menu.setStatus(dto.getStatus());
        menu.setRemark(dto.getRemark());
        baseMapper.updateById(menu);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (baseMapper.selectById(id) == null) throw new SysBizException(SysErrorCode.MENU_NOT_FOUND);
        update(new LambdaUpdateWrapper<SysMenu>().eq(SysMenu::getId, id).set(SysMenu::getStatus, status));
    }

    @Override
    public void changeVisible(Long id, Integer visible) {
        if (baseMapper.selectById(id) == null) throw new SysBizException(SysErrorCode.MENU_NOT_FOUND);
        update(new LambdaUpdateWrapper<SysMenu>().eq(SysMenu::getId, id).set(SysMenu::getVisible, visible));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        if (baseMapper.selectById(id) == null) throw new SysBizException(SysErrorCode.MENU_NOT_FOUND);
        if (baseMapper.countChildren(id) > 0) throw new SysBizException(SysErrorCode.MENU_HAS_CHILDREN);
        removeById(id);
    }

    // ─── 私有辅助 ─────────────────────────────────────────────────────────────

    private List<MenuTreeVO> buildTree(List<SysMenu> all, Long parentId) {
        List<MenuTreeVO> result = new ArrayList<>();
        for (SysMenu menu : all) {
            if (menu.getParentId().equals(parentId)) {
                MenuTreeVO vo = toTreeVO(menu);
                List<MenuTreeVO> children = buildTree(all, menu.getId());
                vo.setChildren(children.isEmpty() ? null : children);
                result.add(vo);
            }
        }
        return result;
    }

    private MenuTreeVO toTreeVO(SysMenu m) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setId(m.getId());
        vo.setParentId(m.getParentId());
        vo.setMenuName(m.getMenuName());
        vo.setMenuType(m.getMenuType());
        vo.setPath(m.getPath());
        vo.setComponent(m.getComponent());
        vo.setPerms(m.getPerms());
        vo.setIcon(m.getIcon());
        vo.setSortOrder(m.getSortOrder());
        vo.setVisible(m.getVisible());
        vo.setStatus(m.getStatus());
        return vo;
    }
}
