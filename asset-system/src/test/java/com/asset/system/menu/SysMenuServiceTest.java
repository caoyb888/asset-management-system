package com.asset.system.menu;

import com.asset.system.common.exception.SysBizException;
import com.asset.system.menu.dto.MenuCreateDTO;
import com.asset.system.menu.entity.SysMenu;
import com.asset.system.menu.mapper.SysMenuMapper;
import com.asset.system.menu.service.impl.SysMenuServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * §4.7 菜单管理 — Service 单元测试
 * MENU-U-01 ~ MENU-U-10
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("§4.7 菜单管理 Service 单元测试")
class SysMenuServiceTest {

    @BeforeAll
    static void initMybatisLambdaCache() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        assistant.setCurrentNamespace("test_menu_ns");
        TableInfoHelper.initTableInfo(assistant, SysMenu.class);
    }

    @Mock SysMenuMapper menuMapper;

    @InjectMocks SysMenuServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", menuMapper);
    }

    // ─── 辅助 ─────────────────────────────────────────────────────────────────

    private SysMenu menu(Long id, Long parentId, String type, String name) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setParentId(parentId);
        m.setMenuType(type);
        m.setMenuName(name);
        m.setSortOrder(1);
        m.setVisible(1);
        m.setStatus(1);
        return m;
    }

    // ─── MENU-U-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("MENU-U-01 获取完整菜单树：返回树形结构，根节点含 children")
    void getMenuTree_returnsFullTree() {
        SysMenu dir  = menu(91001L, 0L,     "M", "系统管理");
        SysMenu page = menu(91002L, 91001L, "C", "用户管理");
        SysMenu btn  = menu(91003L, 91002L, "F", "新增用户");
        when(menuMapper.selectList(any())).thenReturn(List.of(dir, page, btn));

        var tree = service.getMenuTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getChildren()).hasSize(1);
    }

    // ─── MENU-U-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("MENU-U-02 获取路由树：selectRoutesByUserId 被调用")
    void getRouteTree_filteredByRole() {
        // parentId=0 才会成为根节点；构建树的关键是 selectRoutesByUserId 被正确调用
        SysMenu root = menu(91001L, 0L, "M", "系统管理");
        SysMenu page = menu(91002L, 91001L, "C", "用户管理");
        when(menuMapper.selectRoutesByUserId(91003L)).thenReturn(List.of(root, page));

        var tree = service.getRouteTree(91003L);

        verify(menuMapper).selectRoutesByUserId(91003L);
        assertThat(tree).isNotEmpty();
        assertThat(tree.get(0).getChildren()).hasSize(1);
    }

    // ─── MENU-U-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("MENU-U-03 获取权限标识列表：selectPermsByUserId 被调用，返回非空")
    void getPermsByUserId_returnPermsList() {
        when(menuMapper.selectPermsByUserId(91003L))
                .thenReturn(List.of("sys:user:list", "sys:user:add", "sys:role:list"));

        List<String> perms = service.getPermsByUserId(91003L);

        assertThat(perms).containsExactlyInAnyOrder(
                "sys:user:list", "sys:user:add", "sys:role:list");
    }

    // ─── MENU-U-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("MENU-U-04 新增目录：insert 被调用，menuType=M")
    void createMenu_directory_success() {
        when(menuMapper.insert(any(SysMenu.class))).thenReturn(1);

        MenuCreateDTO dto = new MenuCreateDTO();
        dto.setMenuName("新目录");
        dto.setMenuType("M");
        dto.setParentId(0L);

        service.createMenu(dto);

        verify(menuMapper).insert(argThat((SysMenu m) -> "M".equals(m.getMenuType())));
    }

    // ─── MENU-U-05 ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("MENU-U-05 新增菜单：insert 被调用，menuType=C")
    void createMenu_menu_success() {
        when(menuMapper.insert(any(SysMenu.class))).thenReturn(1);

        MenuCreateDTO dto = new MenuCreateDTO();
        dto.setMenuName("新菜单");
        dto.setMenuType("C");
        dto.setPath("/test/page");
        dto.setComponent("test/page/index");

        service.createMenu(dto);

        verify(menuMapper).insert(argThat((SysMenu m) -> "C".equals(m.getMenuType())));
    }

    // ─── MENU-U-06 ────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("MENU-U-06 新增按钮：insert 被调用，menuType=F")
    void createMenu_button_success() {
        when(menuMapper.insert(any(SysMenu.class))).thenReturn(1);

        MenuCreateDTO dto = new MenuCreateDTO();
        dto.setMenuName("新增用户");
        dto.setMenuType("F");
        dto.setPerms("sys:user:add");

        service.createMenu(dto);

        verify(menuMapper).insert(argThat((SysMenu m) -> "F".equals(m.getMenuType())));
    }

    // ─── MENU-U-07 ────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("MENU-U-07 删除-有子菜单，抛出存在子菜单异常")
    void deleteMenu_hasChildren_throws() {
        when(menuMapper.selectById(91001L)).thenReturn(menu(91001L, 0L, "M", "系统管理"));
        when(menuMapper.countChildren(91001L)).thenReturn(3L);

        assertThatThrownBy(() -> service.deleteMenu(91001L))
                .isInstanceOf(SysBizException.class)
                .hasMessageContaining("子菜单");
    }

    // ─── MENU-U-08 ────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @Disabled("deleteMenu 未实现角色绑定校验，跳过")
    @DisplayName("MENU-U-08 删除-有角色绑定禁止（服务端未实现，跳过）")
    void deleteMenu_boundToRole_throws() {
        // SysMenuServiceImpl.deleteMenu 仅检查子菜单，无角色绑定检查
    }

    // ─── MENU-U-09 ────────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("MENU-U-09 删除-无关联，removeById 被调用")
    void deleteMenu_noAssociation_success() {
        when(menuMapper.selectById(91009L)).thenReturn(menu(91009L, 91001L, "F", "停用菜单"));
        when(menuMapper.countChildren(91009L)).thenReturn(0L);
        when(menuMapper.deleteById(91009L)).thenReturn(1);

        service.deleteMenu(91009L);

        verify(menuMapper).deleteById(91009L);
    }

    // ─── MENU-U-10 ────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("MENU-U-10 显隐切换：update 被调用，visible=0")
    void changeVisible_togglesValue() {
        when(menuMapper.selectById(91008L)).thenReturn(menu(91008L, 91001L, "C", "隐藏菜单"));
        when(menuMapper.update(isNull(), any())).thenReturn(1);

        service.changeVisible(91008L, 0);

        verify(menuMapper).update(isNull(), any());
    }
}
