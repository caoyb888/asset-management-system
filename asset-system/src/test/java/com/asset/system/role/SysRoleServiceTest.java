package com.asset.system.role;

import com.asset.system.common.exception.SysBizException;
import com.asset.system.role.dto.RoleCreateDTO;
import com.asset.system.role.entity.SysRole;
import com.asset.system.role.entity.SysRoleData;
import com.asset.system.role.entity.SysRoleMenu;
import com.asset.system.role.mapper.SysRoleDataMapper;
import com.asset.system.role.mapper.SysRoleMapper;
import com.asset.system.role.mapper.SysRoleMenuMapper;
import com.asset.system.role.mapper.SysUserRoleMapper;
import com.asset.system.role.service.impl.SysRoleServiceImpl;
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
 * §4.5 角色管理 — Service 单元测试
 * ROLE-U-01 ~ ROLE-U-11
 *
 * 不启动 Spring 容器，所有依赖通过 Mockito 注入。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("§4.5 角色管理 Service 单元测试")
class SysRoleServiceTest {

    @BeforeAll
    static void initMybatisLambdaCache() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        assistant.setCurrentNamespace("test_role_ns");
        TableInfoHelper.initTableInfo(assistant, SysRole.class);
        TableInfoHelper.initTableInfo(assistant, SysRoleMenu.class);
        TableInfoHelper.initTableInfo(assistant, SysRoleData.class);
    }

    @Mock SysRoleMapper     roleMapper;
    @Mock SysRoleMenuMapper roleMenuMapper;
    @Mock SysRoleDataMapper roleDataMapper;
    @Mock SysUserRoleMapper userRoleMapper;

    @InjectMocks SysRoleServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", roleMapper);
    }

    // ─── 辅助 ─────────────────────────────────────────────────────────────────

    private SysRole stubRole(Long id, String code, String name, Integer dataScope) {
        SysRole r = new SysRole();
        r.setId(id);
        r.setRoleCode(code);
        r.setRoleName(name);
        r.setDataScope(dataScope);
        r.setStatus(1);
        r.setSortOrder(1);
        return r;
    }

    // ─── ROLE-U-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("ROLE-U-01 新增角色成功，insert 被调用")
    void createRole_success() {
        when(roleMapper.selectCount(any())).thenReturn(0L);
        when(roleMapper.insert(any(SysRole.class))).thenReturn(1);

        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setRoleCode("NEW_ROLE");
        dto.setRoleName("新角色");

        service.createRole(dto);

        verify(roleMapper).insert(any(SysRole.class));
    }

    // ─── ROLE-U-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("ROLE-U-02 新增-编码重复，抛出编码已存在异常")
    void createRole_duplicateCode_throws() {
        when(roleMapper.selectCount(any())).thenReturn(1L);

        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setRoleCode("TEST_SUPER_ADMIN");
        dto.setRoleName("超管副本");

        assertThatThrownBy(() -> service.createRole(dto))
                .isInstanceOf(SysBizException.class)
                .hasMessageContaining("已存在");
    }

    // ─── ROLE-U-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("ROLE-U-03 删除-有关联用户，抛出存在关联用户异常")
    void deleteRole_hasUsers_throws() {
        when(roleMapper.selectById(91002L)).thenReturn(stubRole(91002L, "TEST_AREA_MANAGER", "区域经理", 3));
        when(userRoleMapper.countByRoleId(91002L)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteRole(91002L))
                .isInstanceOf(SysBizException.class)
                .hasMessageContaining("用户");
    }

    // ─── ROLE-U-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("ROLE-U-04 删除-级联清理菜单和数据权限")
    void deleteRole_cascadeDeleteMenuAndData() {
        when(roleMapper.selectById(91005L)).thenReturn(stubRole(91005L, "TEST_EMPLOYEE", "普通员工", 5));
        when(userRoleMapper.countByRoleId(91005L)).thenReturn(0L);
        when(roleMapper.deleteById(91005L)).thenReturn(1);
        when(roleMenuMapper.deleteByRoleId(91005L)).thenReturn(0);
        when(roleDataMapper.deleteByRoleId(91005L)).thenReturn(0);

        service.deleteRole(91005L);

        verify(roleMapper).deleteById(91005L);
        verify(roleMenuMapper).deleteByRoleId(91005L);
        verify(roleDataMapper).deleteByRoleId(91005L);
    }

    // ─── ROLE-U-05 ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("ROLE-U-05 分配菜单权限-先删后插")
    void grantMenus_deleteAndReinsert() {
        when(roleMapper.selectById(91002L)).thenReturn(stubRole(91002L, "TEST_AREA_MANAGER", "区域经理", 3));
        when(roleMenuMapper.deleteByRoleId(91002L)).thenReturn(3);
        when(roleMenuMapper.insert(any(SysRoleMenu.class))).thenReturn(1);

        service.grantMenus(91002L, List.of(91001L, 91002L));

        verify(roleMenuMapper).deleteByRoleId(91002L);
        verify(roleMenuMapper, times(2)).insert(any(SysRoleMenu.class));
    }

    // ─── ROLE-U-06 ────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("ROLE-U-06 获取已分配菜单ID，返回正确列表")
    void getMenuIds_returnsCorrectList() {
        when(roleMapper.selectById(91002L)).thenReturn(stubRole(91002L, "TEST_AREA_MANAGER", "区域经理", 3));
        when(roleMenuMapper.selectMenuIdsByRoleId(91002L)).thenReturn(List.of(91001L, 91002L, 91003L));

        List<Long> menuIds = service.getMenuIds(91002L);

        assertThat(menuIds).containsExactly(91001L, 91002L, 91003L);
    }

    // ─── ROLE-U-07 ────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("ROLE-U-07 设置数据权限-全部(scope=1)，不插入 sys_role_data")
    void setDataScope_all_noDeptIds() {
        when(roleMapper.selectById(91002L)).thenReturn(stubRole(91002L, "TEST_AREA_MANAGER", "区域经理", 3));
        when(roleMapper.update(isNull(), any())).thenReturn(1);
        when(roleDataMapper.deleteByRoleId(91002L)).thenReturn(0);

        service.setDataScope(91002L, 1, null);

        verify(roleDataMapper).deleteByRoleId(91002L);
        verify(roleDataMapper, never()).insert(any(SysRoleData.class));
    }

    // ─── ROLE-U-08 ────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("ROLE-U-08 设置数据权限-自定义(scope=2)，插入2条 sys_role_data")
    void setDataScope_custom_insertDeptIds() {
        when(roleMapper.selectById(91004L)).thenReturn(stubRole(91004L, "TEST_CUSTOM_SCOPE", "自定义", 2));
        when(roleMapper.update(isNull(), any())).thenReturn(1);
        when(roleDataMapper.deleteByRoleId(91004L)).thenReturn(2);
        when(roleDataMapper.insert(any(SysRoleData.class))).thenReturn(1);

        service.setDataScope(91004L, 2, List.of(91003L, 91004L));

        verify(roleDataMapper).deleteByRoleId(91004L);
        verify(roleDataMapper, times(2)).insert(any(SysRoleData.class));
    }

    // ─── ROLE-U-09 ────────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("ROLE-U-09 设置数据权限-仅本人(scope=5)，不插入 sys_role_data")
    void setDataScope_self_noDeptIds() {
        when(roleMapper.selectById(91005L)).thenReturn(stubRole(91005L, "TEST_EMPLOYEE", "普通员工", 5));
        when(roleMapper.update(isNull(), any())).thenReturn(1);
        when(roleDataMapper.deleteByRoleId(91005L)).thenReturn(0);

        service.setDataScope(91005L, 5, List.of(91003L));

        verify(roleDataMapper).deleteByRoleId(91005L);
        // scope!=2，不插入
        verify(roleDataMapper, never()).insert(any(SysRoleData.class));
    }

    // ─── ROLE-U-10 ────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("ROLE-U-10 获取自定义数据权限部门ID，返回 [91003, 91004]")
    void getDeptIds_customScope_returnsDeptList() {
        when(roleDataMapper.selectDeptIdsByRoleId(91004L)).thenReturn(List.of(91003L, 91004L));

        List<Long> deptIds = service.getDeptIds(91004L);

        assertThat(deptIds).containsExactly(91003L, 91004L);
    }

    // ─── ROLE-U-11 ────────────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("ROLE-U-11 编辑-编码未变，update 被调用，不抛异常")
    void updateRole_sameCode_skipCheck() {
        // updateRole 不做编码唯一性校验，直接 update
        when(roleMapper.selectById(91002L)).thenReturn(stubRole(91002L, "TEST_AREA_MANAGER", "区域经理", 3));
        when(roleMapper.update(isNull(), any())).thenReturn(1);

        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setId(91002L);
        dto.setRoleCode("TEST_AREA_MANAGER"); // 编码未变
        dto.setRoleName("测试区域经理（已更新）");

        assertThatNoException().isThrownBy(() -> service.updateRole(dto));
        verify(roleMapper).update(isNull(), any());
    }
}
