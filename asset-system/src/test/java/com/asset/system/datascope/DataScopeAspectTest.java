package com.asset.system.datascope;

import com.asset.common.security.util.LoginUser;
import com.asset.system.common.datascope.DataScopeAspect;
import com.asset.system.common.datascope.DataScopeContext;
import com.asset.system.common.datascope.DataScopeInfo;
import com.asset.system.dept.entity.SysDept;
import com.asset.system.dept.mapper.SysDeptMapper;
import com.asset.system.role.entity.SysRole;
import com.asset.system.role.mapper.SysRoleDataMapper;
import com.asset.system.role.mapper.SysRoleMapper;
import com.asset.system.user.entity.SysUser;
import com.asset.system.user.mapper.SysUserMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * §4.6 数据权限引擎 — DataScopeAspect 单元测试
 * DS-U-01 ~ DS-U-12
 *
 * 策略：将 LoginUser 注入 SecurityContextHolder 模拟已登录用户；
 * 通过 ProceedingJoinPoint.proceed() 的 thenAnswer 捕获切面写入的 DataScopeInfo。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("§4.6 数据权限引擎单元测试")
class DataScopeAspectTest {

    @Mock SysUserMapper     userMapper;
    @Mock SysRoleMapper     roleMapper;
    @Mock SysRoleDataMapper roleDataMapper;
    @Mock SysDeptMapper     deptMapper;
    @Mock ProceedingJoinPoint joinPoint;

    @InjectMocks DataScopeAspect aspect;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        DataScopeContext.clear();
    }

    // ─── 辅助 ─────────────────────────────────────────────────────────────────

    /** 向 SecurityContextHolder 注入模拟登录用户 */
    private void loginAs(Long userId) {
        LoginUser loginUser = new LoginUser(userId, "testuser", "", List.of());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /** 构造 SysRole stub */
    private SysRole role(Long id, String code, Integer dataScope) {
        SysRole r = new SysRole();
        r.setId(id);
        r.setRoleCode(code);
        r.setDataScope(dataScope);
        r.setStatus(1);
        return r;
    }

    /** 构造 SysUser stub */
    private SysUser user(Long id, Long deptId) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setDeptId(deptId);
        return u;
    }

    /** 构造 SysDept stub */
    private SysDept dept(Long id) {
        SysDept d = new SysDept();
        d.setId(id);
        return d;
    }

    /**
     * 调用切面并在 proceed() 内捕获 DataScopeContext，
     * 保证在 finally 清除前读取到值。
     */
    private DataScopeInfo runAspect() throws Throwable {
        AtomicReference<DataScopeInfo> ref = new AtomicReference<>();
        when(joinPoint.proceed()).thenAnswer(inv -> {
            ref.set(DataScopeContext.get());
            return null;
        });
        aspect.around(joinPoint);
        return ref.get();
    }

    // ─── DS-U-01 ──────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("DS-U-01 SUPER_ADMIN 角色：admin=true，不注入 WHERE")
    void dataScope_superAdmin_noRestriction() throws Throwable {
        loginAs(91001L);
        when(userMapper.selectById(91001L)).thenReturn(user(91001L, 91001L));
        when(roleMapper.selectByUserId(91001L)).thenReturn(
                List.of(role(91001L, "SUPER_ADMIN", 1)));

        DataScopeInfo info = runAspect();

        assertThat(info.isAdmin()).isTrue();
        assertThat(info.getUserId()).isEqualTo(91001L);
    }

    // ─── DS-U-02 ──────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("DS-U-02 dataScope=1 全部数据：admin=true")
    void dataScope_all_noRestriction() throws Throwable {
        loginAs(91001L);
        when(userMapper.selectById(91001L)).thenReturn(user(91001L, 91001L));
        when(roleMapper.selectByUserId(91001L)).thenReturn(
                List.of(role(91001L, "ANY_ROLE", 1)));

        DataScopeInfo info = runAspect();

        assertThat(info.isAdmin()).isTrue();
    }

    // ─── DS-U-03 ──────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("DS-U-03 dataScope=3 本机构：deptIds=[91002]")
    void dataScope_dept_filterByUserDept() throws Throwable {
        loginAs(91002L);
        when(userMapper.selectById(91002L)).thenReturn(user(91002L, 91002L));
        when(roleMapper.selectByUserId(91002L)).thenReturn(
                List.of(role(91002L, "AREA_MANAGER", 3)));

        DataScopeInfo info = runAspect();

        assertThat(info.isAdmin()).isFalse();
        assertThat(info.isSelfOnly()).isFalse();
        assertThat(info.getDeptIds()).containsExactly(91002L);
    }

    // ─── DS-U-04 ──────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("DS-U-04 dataScope=4 本机构及下级：deptIds=[91002,91003,91004]")
    void dataScope_deptAndChild_filterByDeptTree() throws Throwable {
        loginAs(91003L);
        when(userMapper.selectById(91003L)).thenReturn(user(91003L, 91002L));
        when(roleMapper.selectByUserId(91003L)).thenReturn(
                List.of(role(91003L, "PROJECT_MANAGER", 4)));
        when(deptMapper.selectDescendants(91002L)).thenReturn(
                List.of(dept(91003L), dept(91004L)));

        DataScopeInfo info = runAspect();

        assertThat(info.isAdmin()).isFalse();
        assertThat(info.getDeptIds()).containsExactlyInAnyOrder(91002L, 91003L, 91004L);
    }

    // ─── DS-U-05 ──────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("DS-U-05 dataScope=2 自定义：deptIds=[91003,91004]")
    void dataScope_custom_filterByRoleData() throws Throwable {
        loginAs(91004L);
        when(userMapper.selectById(91004L)).thenReturn(user(91004L, 91002L));
        when(roleMapper.selectByUserId(91004L)).thenReturn(
                List.of(role(91004L, "CUSTOM_SCOPE", 2)));
        when(roleDataMapper.selectDeptIdsByRoleId(91004L)).thenReturn(
                List.of(91003L, 91004L));

        DataScopeInfo info = runAspect();

        assertThat(info.isAdmin()).isFalse();
        assertThat(info.getDeptIds()).containsExactlyInAnyOrder(91003L, 91004L);
    }

    // ─── DS-U-06 ──────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("DS-U-06 dataScope=5 仅本人：selfOnly=true")
    void dataScope_self_filterByUserId() throws Throwable {
        loginAs(91005L);
        when(userMapper.selectById(91005L)).thenReturn(user(91005L, 91003L));
        when(roleMapper.selectByUserId(91005L)).thenReturn(
                List.of(role(91005L, "EMPLOYEE", 5)));

        DataScopeInfo info = runAspect();

        assertThat(info.isAdmin()).isFalse();
        assertThat(info.isSelfOnly()).isTrue();
        assertThat(info.getDeptIds()).isNull();
    }

    // ─── DS-U-07 ──────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("DS-U-07 多角色含全部(scope=1)：取最大权限→admin=true")
    void dataScope_multiRole_maxScopeAll() throws Throwable {
        loginAs(91002L);
        when(userMapper.selectById(91002L)).thenReturn(user(91002L, 91002L));
        // roleA=scope3, roleB=scope1 → 含 scope=1 → admin
        when(roleMapper.selectByUserId(91002L)).thenReturn(List.of(
                role(91002L, "AREA_MANAGER", 3),
                role(91099L, "ADMIN_BACKUP", 1)));

        DataScopeInfo info = runAspect();

        assertThat(info.isAdmin()).isTrue();
    }

    // ─── DS-U-08 ──────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("DS-U-08 多角色部门并集：roleA(scope=3,dept=91002) + roleB(scope=2,deptIds=[91005]) → [91002,91005]")
    void dataScope_multiRole_unionDepts() throws Throwable {
        loginAs(91002L);
        when(userMapper.selectById(91002L)).thenReturn(user(91002L, 91002L));
        SysRole roleA = role(91002L, "AREA_MANAGER", 3);  // scope=3 → user.deptId=91002
        SysRole roleB = role(91099L, "CUSTOM_EXTRA", 2);  // scope=2 → custom deptIds
        when(roleMapper.selectByUserId(91002L)).thenReturn(List.of(roleA, roleB));
        when(roleDataMapper.selectDeptIdsByRoleId(91099L)).thenReturn(List.of(91005L));

        DataScopeInfo info = runAspect();

        assertThat(info.isAdmin()).isFalse();
        assertThat(info.getDeptIds()).containsExactlyInAnyOrder(91002L, 91005L);
    }

    // ─── DS-U-09 ──────────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("DS-U-09 多角色 本人+本部门：部门范围 > 仅本人 → deptIds=[91002]")
    void dataScope_multiRole_selfPlusDept() throws Throwable {
        loginAs(91005L);
        when(userMapper.selectById(91005L)).thenReturn(user(91005L, 91002L));
        // roleA=scope5(self), roleB=scope3(dept)
        when(roleMapper.selectByUserId(91005L)).thenReturn(List.of(
                role(91005L, "EMPLOYEE", 5),
                role(91002L, "AREA_MANAGER", 3)));

        DataScopeInfo info = runAspect();

        // deptIdSet non-empty → selfOnly overridden
        assertThat(info.isAdmin()).isFalse();
        assertThat(info.isSelfOnly()).isFalse();
        assertThat(info.getDeptIds()).containsExactly(91002L);
    }

    // ─── DS-U-10 ──────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("DS-U-10 方法执行完毕后 ThreadLocal 自动清除")
    void dataScope_afterMethod_contextCleared() throws Throwable {
        loginAs(91001L);
        when(userMapper.selectById(91001L)).thenReturn(user(91001L, 91001L));
        when(roleMapper.selectByUserId(91001L)).thenReturn(
                List.of(role(91001L, "SUPER_ADMIN", 1)));
        when(joinPoint.proceed()).thenReturn(null);

        aspect.around(joinPoint);

        assertThat(DataScopeContext.get()).isNull();
    }

    // ─── DS-U-11 ──────────────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("DS-U-11 自定义权限-空部门列表：deptIds=[]，查询返回空")
    void dataScope_custom_emptyDepts_noData() throws Throwable {
        loginAs(91004L);
        when(userMapper.selectById(91004L)).thenReturn(user(91004L, 91002L));
        when(roleMapper.selectByUserId(91004L)).thenReturn(
                List.of(role(91004L, "CUSTOM_SCOPE", 2)));
        when(roleDataMapper.selectDeptIdsByRoleId(91004L)).thenReturn(List.of());

        DataScopeInfo info = runAspect();

        assertThat(info.isAdmin()).isFalse();
        assertThat(info.isSelfOnly()).isFalse();
        assertThat(info.getDeptIds()).isEmpty();
    }

    // ─── DS-U-12 ──────────────────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("DS-U-12 未标注@DataScope方法：切面不拦截，DataScopeContext 为 null")
    void dataScope_noAnnotation_noInterception() {
        // 不调用 aspect.around()，上下文应保持 null
        DataScopeContext.clear();
        assertThat(DataScopeContext.get()).isNull();
    }
}
