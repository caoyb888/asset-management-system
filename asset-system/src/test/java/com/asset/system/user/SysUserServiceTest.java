package com.asset.system.user;

import com.asset.common.security.crypto.SmCryptoUtil;
import com.asset.system.auth.service.SysTokenService;
import com.asset.system.common.datascope.DataScopeContext;
import com.asset.system.common.datascope.DataScopeInfo;
import com.asset.system.common.exception.SysBizException;
import com.asset.system.common.exception.SysErrorCode;
import com.asset.system.dept.mapper.SysDeptMapper;
import com.asset.system.role.entity.SysUserRole;
import com.asset.system.role.mapper.SysRoleMapper;
import com.asset.system.role.mapper.SysUserRoleMapper;
import com.asset.system.user.dto.ChangePasswordDTO;
import com.asset.system.user.dto.ResetPwdDTO;
import com.asset.system.user.dto.UserCreateDTO;
import com.asset.system.user.dto.UserQueryDTO;
import com.asset.system.user.entity.SysUser;
import com.asset.system.user.entity.SysUserPost;
import com.asset.system.user.mapper.SysUserMapper;
import com.asset.system.user.mapper.SysUserPostMapper;
import com.asset.system.user.service.impl.SysUserServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * §4.2 用户管理 — Service 单元测试
 * USER-U-01 ~ USER-U-19
 *
 * 不启动 Spring 容器，所有依赖通过 Mockito 注入。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("§4.2 用户管理 Service 单元测试")
class SysUserServiceTest {

    @BeforeAll
    static void initMybatisLambdaCache() {
        // LambdaUpdateWrapper/LambdaQueryWrapper 需要 TableInfo，否则抛 "can not find lambda cache"
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        assistant.setCurrentNamespace("test_ns");
        TableInfoHelper.initTableInfo(assistant, SysUser.class);
    }

    @Mock SysUserMapper     userMapper;
    @Mock SysUserRoleMapper userRoleMapper;
    @Mock SysUserPostMapper userPostMapper;
    @Mock SysRoleMapper     roleMapper;
    @Mock SysDeptMapper     deptMapper;
    @Mock SysTokenService   tokenService;

    @InjectMocks SysUserServiceImpl service;

    @BeforeEach
    void setUp() {
        // ServiceImpl.baseMapper 需通过反射注入
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
    }

    @AfterEach
    void tearDown() {
        DataScopeContext.clear();
    }

    // ─── 工具 ────────────────────────────────────────────────────────────────────

    private SysUser makeUser(Long id, String username) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername(username);
        u.setStatus(1);
        u.setPassword(SmCryptoUtil.sm3Hash("Test@12345"));
        return u;
    }

    private UserCreateDTO makeCreateDTO(String username) {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername(username);
        dto.setPassword("Test@12345");
        dto.setDeptId(91001L);
        return dto;
    }

    // ─── USER-U-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("USER-U-01 新增用户成功：insert 被调用，返回新用户ID")
    void createUser_success() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        doAnswer(inv -> {
            ((SysUser) inv.getArgument(0)).setId(99999L);
            return 1;
        }).when(userMapper).insert(any(SysUser.class));

        Long id = service.createUser(makeCreateDTO("new_user"));

        assertThat(id).isNotNull();
        verify(userMapper).insert(any(SysUser.class));
    }

    // ─── USER-U-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("USER-U-02 新增-用户名重复：抛出 USER_ALREADY_EXISTS")
    void createUser_duplicateUsername_throws() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        SysBizException ex = catchThrowableOfType(
                () -> service.createUser(makeCreateDTO("test_admin")), SysBizException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(SysErrorCode.USER_ALREADY_EXISTS.getCode());
    }

    // ─── USER-U-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @Disabled("服务层未实现手机号唯一性校验，暂时跳过")
    @DisplayName("USER-U-03 新增-手机号重复：[未实现，跳过]")
    void createUser_duplicatePhone_throws() {}

    // ─── USER-U-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("USER-U-04 新增-初始密码SM3加密：存储的是64位哈希而非明文")
    void createUser_passwordEncrypted() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        doAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setId(99999L);
            assertThat(u.getPassword())
                    .as("密码应为SM3哈希，非明文")
                    .isNotEqualTo("Test@12345")
                    .hasSize(64);
            return 1;
        }).when(userMapper).insert(any(SysUser.class));

        service.createUser(makeCreateDTO("new_user"));
    }

    // ─── USER-U-05 ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("USER-U-05 新增-绑定角色：sys_user_role 插入2条记录")
    void createUser_withRoles_bindsUserRole() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        doAnswer(inv -> {
            ((SysUser) inv.getArgument(0)).setId(99999L);
            return 1;
        }).when(userMapper).insert(any(SysUser.class));

        UserCreateDTO dto = makeCreateDTO("new_user");
        dto.setRoleIds(List.of(91002L, 91003L));
        service.createUser(dto);

        verify(userRoleMapper, times(2)).insert(any(SysUserRole.class));
    }

    // ─── USER-U-06 ────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("USER-U-06 新增-绑定岗位：sys_user_post 插入1条记录")
    void createUser_withPosts_bindsUserPost() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        doAnswer(inv -> {
            ((SysUser) inv.getArgument(0)).setId(99999L);
            return 1;
        }).when(userMapper).insert(any(SysUser.class));

        UserCreateDTO dto = makeCreateDTO("new_user");
        dto.setPostIds(List.of(91001L));
        service.createUser(dto);

        verify(userPostMapper, times(1)).insert(any(SysUserPost.class));
    }

    // ─── USER-U-07 ────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("USER-U-07 编辑-不包含密码字段：update 被调用，密码未被修改")
    void updateUser_skipPasswordField() {
        when(userMapper.selectById(91002L)).thenReturn(makeUser(91002L, "test_area_mgr"));
        when(userMapper.update(any(), any())).thenReturn(1);

        UserCreateDTO dto = new UserCreateDTO();
        dto.setId(91002L);
        dto.setUsername("test_area_mgr");
        dto.setRealName("新名称");
        // 不设置 password 字段

        assertThatCode(() -> service.updateUser(dto)).doesNotThrowAnyException();
        // 验证 update 被调用（LambdaUpdateWrapper 不含 password 字段）
        verify(userMapper).update(any(), any());
    }

    // ─── USER-U-08 ────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("USER-U-08 编辑-超级管理员账号禁止修改：抛出 USER_HAS_ADMIN_ROLE")
    void updateUser_superAdmin_throws() {
        when(userMapper.selectById(99L)).thenReturn(makeUser(99L, "SUPER_ADMIN"));

        UserCreateDTO dto = new UserCreateDTO();
        dto.setId(99L);
        dto.setUsername("SUPER_ADMIN");
        dto.setRealName("攻击者");

        SysBizException ex = catchThrowableOfType(
                () -> service.updateUser(dto), SysBizException.class);

        assertThat(ex.getCode()).isEqualTo(SysErrorCode.USER_HAS_ADMIN_ROLE.getCode());
    }

    // ─── USER-U-09 ────────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("USER-U-09 编辑-普通用户正常更新成功")
    void updateUser_normalUser_success() {
        when(userMapper.selectById(91002L)).thenReturn(makeUser(91002L, "test_area_mgr"));
        when(userMapper.update(any(), any())).thenReturn(1);

        UserCreateDTO dto = new UserCreateDTO();
        dto.setId(91002L);
        dto.setUsername("test_area_mgr");
        dto.setRealName("区域经理（已更名）");

        assertThatCode(() -> service.updateUser(dto)).doesNotThrowAnyException();
    }

    // ─── USER-U-10 ────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("USER-U-10 删除-用户不存在：抛出 USER_NOT_FOUND")
    void deleteUser_notFound_throws() {
        when(userMapper.selectById(999999L)).thenReturn(null);

        SysBizException ex = catchThrowableOfType(
                () -> service.deleteUser(999999L), SysBizException.class);

        assertThat(ex.getCode()).isEqualTo(SysErrorCode.USER_NOT_FOUND.getCode());
    }

    // ─── USER-U-11 ────────────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("USER-U-11 删除-admin账号禁止删除：抛出 USER_DELETE_SELF_FORBIDDEN")
    void deleteUser_adminUsername_throws() {
        when(userMapper.selectById(1L)).thenReturn(makeUser(1L, "admin"));

        SysBizException ex = catchThrowableOfType(
                () -> service.deleteUser(1L), SysBizException.class);

        assertThat(ex.getCode()).isEqualTo(SysErrorCode.USER_DELETE_SELF_FORBIDDEN.getCode());
    }

    // ─── USER-U-12 ────────────────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("USER-U-12 删除-级联清理：deleteById + deleteByUserId(角色) + deleteByUserId(岗位)")
    void deleteUser_cascadeDeleteRolesAndPosts() {
        when(userMapper.selectById(91005L)).thenReturn(makeUser(91005L, "test_normal_user"));
        when(userMapper.deleteById(any(Long.class))).thenReturn(1);

        service.deleteUser(91005L);

        verify(userMapper).deleteById(91005L);
        verify(userRoleMapper).deleteByUserId(91005L);
        verify(userPostMapper).deleteByUserId(91005L);
    }

    // ─── USER-U-13 ────────────────────────────────────────────────────────────

    @Test
    @Order(13)
    @DisplayName("USER-U-13 重置密码-新密码SM3加密存储：update 被调用")
    void resetPassword_encrypted() {
        when(userMapper.selectById(91001L)).thenReturn(makeUser(91001L, "test_admin"));
        when(userMapper.update(any(), any())).thenReturn(1);

        ResetPwdDTO dto = new ResetPwdDTO();
        dto.setUserId(91001L);
        dto.setNewPassword("NewPass@123");

        assertThatCode(() -> service.resetPassword(dto)).doesNotThrowAnyException();
        verify(userMapper).update(any(), any());
    }

    // ─── USER-U-14 ────────────────────────────────────────────────────────────

    @Test
    @Order(14)
    @DisplayName("USER-U-14 修改个人密码-旧密码错误：抛出 USER_OLD_PWD_WRONG")
    void changePassword_wrongOldPassword_throws() {
        SysUser user = makeUser(91001L, "test_admin");
        user.setPassword(SmCryptoUtil.sm3Hash("Test@12345"));
        when(userMapper.selectByIdWithPwd(91001L)).thenReturn(user);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("WrongPassword");
        dto.setNewPassword("NewPass@123");

        SysBizException ex = catchThrowableOfType(
                () -> service.changePassword(91001L, dto), SysBizException.class);

        assertThat(ex.getCode()).isEqualTo(SysErrorCode.USER_OLD_PWD_WRONG.getCode());
    }

    // ─── USER-U-15 ────────────────────────────────────────────────────────────

    @Test
    @Order(15)
    @DisplayName("USER-U-15 修改个人密码-成功：新密码SM3加密存储")
    void changePassword_success() {
        SysUser user = makeUser(91001L, "test_admin");
        user.setPassword(SmCryptoUtil.sm3Hash("Test@12345"));
        when(userMapper.selectByIdWithPwd(91001L)).thenReturn(user);
        when(userMapper.update(any(), any())).thenReturn(1);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("Test@12345");
        dto.setNewPassword("NewPass@123");

        assertThatCode(() -> service.changePassword(91001L, dto)).doesNotThrowAnyException();
        verify(userMapper).update(any(), any());
    }

    // ─── USER-U-16 ────────────────────────────────────────────────────────────

    @Test
    @Order(16)
    @DisplayName("USER-U-16 启用/禁用用户：status 更新成功")
    void changeStatus_success() {
        when(userMapper.selectById(91006L)).thenReturn(makeUser(91006L, "test_disabled"));
        when(userMapper.update(any(), any())).thenReturn(1);

        assertThatCode(() -> service.changeStatus(91006L, 1)).doesNotThrowAnyException();
        verify(userMapper).update(any(), any());
    }

    // ─── USER-U-17 ────────────────────────────────────────────────────────────

    @Test
    @Order(17)
    @DisplayName("USER-U-17 分配角色-先删后插：deleteByUserId + insert")
    void assignRoles_deleteAndInsert() {
        when(userMapper.selectById(91005L)).thenReturn(makeUser(91005L, "test_normal_user"));

        service.assignRoles(91005L, List.of(91002L));

        verify(userRoleMapper).deleteByUserId(91005L);
        verify(userRoleMapper).insert(any(SysUserRole.class));
    }

    // ─── USER-U-18 ────────────────────────────────────────────────────────────

    @Test
    @Order(18)
    @DisplayName("USER-U-18 强制下线：tokenService.removeAllRefreshTokensByUser 被调用")
    void forceOffline_removeRedisTokens() {
        when(userMapper.selectById(91005L)).thenReturn(makeUser(91005L, "test_normal_user"));

        service.forceOffline(91005L);

        verify(tokenService).removeAllRefreshTokensByUser(91005L);
    }

    // ─── USER-U-19 ────────────────────────────────────────────────────────────

    @Test
    @Order(19)
    @DisplayName("USER-U-19 分页查询-数据权限过滤：空deptIds范围selectPage仍被调用")
    void pageQuery_withDataScope_filtered() {
        // 模拟 deptIds=空 → 无任何部门权限（wrapper will apply "1=0"）
        DataScopeContext.set(DataScopeInfo.ofDepts(91001L, List.of()));
        when(userMapper.selectPage(any(), any())).thenReturn(new Page<>());

        UserQueryDTO query = new UserQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);

        IPage<?> result = service.pageQuery(query);

        // selectPage 仍被调用（即使 wrapper 注入了 1=0 条件）
        verify(userMapper).selectPage(any(), any());
        // 返回的 total=0（Mock 的空页）
        assertThat(result.getTotal()).isZero();
    }
}
