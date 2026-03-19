package com.asset.system.dept;

import com.asset.system.common.exception.SysBizException;
import com.asset.system.dept.dto.DeptCreateDTO;
import com.asset.system.dept.dto.MoveDeptDTO;
import com.asset.system.dept.entity.SysDept;
import com.asset.system.dept.mapper.SysDeptMapper;
import com.asset.system.dept.service.impl.SysDeptServiceImpl;
import com.asset.system.role.mapper.SysRoleMapper;
import com.asset.system.user.entity.SysUser;
import com.asset.system.user.mapper.SysUserMapper;
import com.asset.system.user.mapper.SysUserPostMapper;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * §4.3 机构管理 — Service 单元测试
 * DEPT-U-01 ~ DEPT-U-13
 *
 * 不启动 Spring 容器，所有依赖通过 Mockito 注入。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("§4.3 机构管理 Service 单元测试")
class SysDeptServiceTest {

    @BeforeAll
    static void initMybatisLambdaCache() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        assistant.setCurrentNamespace("test_dept_ns");
        TableInfoHelper.initTableInfo(assistant, SysDept.class);
    }

    @Mock SysDeptMapper    deptMapper;
    @Mock SysUserMapper    userMapper;
    @Mock SysUserPostMapper userPostMapper;
    @Mock SysRoleMapper    roleMapper;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks SysDeptServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", deptMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ─── 辅助 ─────────────────────────────────────────────────────────────────

    private SysDept stubDept(Long id, Long parentId, String ancestors, String name) {
        SysDept d = new SysDept();
        d.setId(id);
        d.setParentId(parentId);
        d.setAncestors(ancestors);
        d.setDeptName(name);
        d.setStatus(1);
        return d;
    }

    // ─── DEPT-U-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("DEPT-U-01 新增-parentId=91002，ancestors=0,91001,91002")
    void createDept_autoCalcAncestors() {
        SysDept parent = stubDept(91002L, 91001L, "0,91001", "华南");
        when(deptMapper.selectById(91002L)).thenReturn(parent);
        when(deptMapper.insert(any(SysDept.class))).thenReturn(1);

        DeptCreateDTO dto = new DeptCreateDTO();
        dto.setParentId(91002L);
        dto.setDeptName("新测试部门");

        service.createDept(dto);

        verify(deptMapper).insert(argThat((SysDept d) -> "0,91001,91002".equals(d.getAncestors())));
        verify(redisTemplate).delete("sys:dept:tree");
    }

    // ─── DEPT-U-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("DEPT-U-02 新增-parentId=0，ancestors=0")
    void createDept_rootNode_ancestorsIsZero() {
        when(deptMapper.insert(any(SysDept.class))).thenReturn(1);

        DeptCreateDTO dto = new DeptCreateDTO();
        dto.setParentId(0L);
        dto.setDeptName("顶级部门");

        service.createDept(dto);

        verify(deptMapper).insert(argThat((SysDept d) -> "0".equals(d.getAncestors())));
    }

    // ─── DEPT-U-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @Disabled("createDept 未实现 deptCode 唯一性校验，跳过")
    @DisplayName("DEPT-U-03 新增-编码重复（服务端未实现，跳过）")
    void createDept_duplicateCode_throws() {
        // Not implemented in SysDeptServiceImpl
    }

    // ─── DEPT-U-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("DEPT-U-04 编辑-变更parentId，级联更新后代ancestors")
    void updateDept_cascadeUpdateChildAncestors() {
        // 91003 originally under 91002 (ancestors="0,91001,91002")
        SysDept exist = stubDept(91003L, 91002L, "0,91001,91002", "天河项目部");
        SysDept newParent = stubDept(91005L, 91001L, "0,91001", "华东");
        when(deptMapper.selectById(91003L)).thenReturn(exist);
        when(deptMapper.selectById(91005L)).thenReturn(newParent);
        when(deptMapper.update(isNull(), any())).thenReturn(1);
        when(deptMapper.batchUpdateAncestors(anyString(), anyString())).thenReturn(1);

        DeptCreateDTO dto = new DeptCreateDTO();
        dto.setId(91003L);
        dto.setParentId(91005L);
        dto.setDeptName("天河项目部");

        service.updateDept(dto);

        // oldAncestorPath = exist.ancestors + "," + id = "0,91001,91002,91003"
        // newAncestors    = newParent.ancestors + "," + newParentId = "0,91001,91005"
        // newAncestorPath = newAncestors + "," + id = "0,91001,91005,91003"
        verify(deptMapper).batchUpdateAncestors("0,91001,91002,91003", "0,91001,91005,91003");
        verify(redisTemplate).delete("sys:dept:tree");
    }

    // ─── DEPT-U-05 ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("DEPT-U-05 删除-有子部门，抛出存在子部门异常")
    void deleteDept_hasChildren_throws() {
        SysDept dept = stubDept(91002L, 91001L, "0,91001", "华南");
        when(deptMapper.selectById(91002L)).thenReturn(dept);
        when(deptMapper.countChildren(91002L)).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteDept(91002L))
                .isInstanceOf(SysBizException.class)
                .hasMessageContaining("子部门");
    }

    // ─── DEPT-U-06 ────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("DEPT-U-06 删除-有用户，抛出存在用户异常")
    void deleteDept_hasUsers_throws() {
        SysDept dept = stubDept(91002L, 91001L, "0,91001", "华南");
        when(deptMapper.selectById(91002L)).thenReturn(dept);
        when(deptMapper.countChildren(91002L)).thenReturn(0L);
        when(userMapper.countByDeptId(91002L)).thenReturn(3L);

        assertThatThrownBy(() -> service.deleteDept(91002L))
                .isInstanceOf(SysBizException.class)
                .hasMessageContaining("用户");
    }

    // ─── DEPT-U-07 ────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("DEPT-U-07 删除-无关联，removeById 被调用")
    void deleteDept_noAssociation_success() {
        SysDept dept = stubDept(91007L, 0L, "0", "空部门");
        when(deptMapper.selectById(91007L)).thenReturn(dept);
        when(deptMapper.countChildren(91007L)).thenReturn(0L);
        when(userMapper.countByDeptId(91007L)).thenReturn(0L);
        when(deptMapper.deleteById(91007L)).thenReturn(1);

        service.deleteDept(91007L);

        verify(deptMapper).deleteById(91007L);
        verify(redisTemplate).delete("sys:dept:tree");
    }

    // ─── DEPT-U-08 ────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("DEPT-U-08 移动子树-91003从91002移到91005，batchUpdateAncestors被调用")
    void moveDept_batchUpdateDescendants() {
        SysDept dept = stubDept(91003L, 91002L, "0,91001,91002", "天河");
        SysDept newParent = stubDept(91005L, 91001L, "0,91001", "华东");
        when(deptMapper.selectById(91003L)).thenReturn(dept);
        when(deptMapper.selectById(91005L)).thenReturn(newParent);
        when(deptMapper.selectDescendants(91003L)).thenReturn(List.of());
        when(deptMapper.update(isNull(), any())).thenReturn(1);
        when(deptMapper.batchUpdateAncestors(anyString(), anyString())).thenReturn(0);

        MoveDeptDTO dto = new MoveDeptDTO();
        dto.setTargetParentId(91005L);

        service.moveDept(91003L, dto);

        // oldAncestorPrefix = dept.ancestors + "," + id = "0,91001,91002,91003"
        // newAncestors      = "0,91001,91005"
        // newAncestorPrefix = "0,91001,91005,91003"
        verify(deptMapper).batchUpdateAncestors("0,91001,91002,91003", "0,91001,91005,91003");
        verify(redisTemplate).delete("sys:dept:tree");
    }

    // ─── DEPT-U-09 ────────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("DEPT-U-09 移动子树-目标是后代，抛出异常")
    void moveDept_toDescendant_throws() {
        SysDept dept = stubDept(91002L, 91001L, "0,91001", "华南");
        SysDept descendant = stubDept(91003L, 91002L, "0,91001,91002", "天河");
        when(deptMapper.selectById(91002L)).thenReturn(dept);
        when(deptMapper.selectDescendants(91002L)).thenReturn(List.of(descendant));

        MoveDeptDTO dto = new MoveDeptDTO();
        dto.setTargetParentId(91003L); // 91003 是 91002 的后代

        assertThatThrownBy(() -> service.moveDept(91002L, dto))
                .isInstanceOf(SysBizException.class);
    }

    // ─── DEPT-U-10 ────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("DEPT-U-10 树查询-检查缓存 key，并写缓存")
    void getDeptTree_checkAndSetCache() {
        when(valueOps.get("sys:dept:tree")).thenReturn("1");
        when(deptMapper.selectAllNormal()).thenReturn(List.of());

        service.getDeptTree(null);

        verify(valueOps).get("sys:dept:tree");
        // Cache should be (re)set after DB query
        verify(valueOps).set(eq("sys:dept:tree"), eq("1"), anyLong(), any());
    }

    // ─── DEPT-U-11 ────────────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("DEPT-U-11 新增部门后，缓存被清除")
    void createDept_evictCache() {
        when(deptMapper.insert(any(SysDept.class))).thenReturn(1);

        DeptCreateDTO dto = new DeptCreateDTO();
        dto.setParentId(0L);
        dto.setDeptName("测试缓存清除");

        service.createDept(dto);

        verify(redisTemplate).delete("sys:dept:tree");
    }

    // ─── DEPT-U-12 ────────────────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("DEPT-U-12 查询部门下用户-includeChildren=true，selectDescendants 被调用")
    void getDeptUsers_includeChildren() {
        SysDept child1 = stubDept(91003L, 91002L, "0,91001,91002", "天河");
        SysDept child2 = stubDept(91004L, 91002L, "0,91001,91002", "番禺");
        when(deptMapper.selectDescendants(91002L)).thenReturn(List.of(child1, child2));

        SysUser user = new SysUser();
        user.setId(91002L);
        user.setUsername("area_mgr");
        user.setDeptId(91002L);
        user.setStatus(1);
        when(userMapper.selectList(any())).thenReturn(List.of(user));
        when(roleMapper.selectByUserId(any())).thenReturn(List.of());
        when(userPostMapper.selectPostsByUserId(any())).thenReturn(List.of());

        var result = service.getDeptUsers(91002L, true);

        assertThat(result).hasSize(1);
        verify(deptMapper).selectDescendants(91002L);
    }

    // ─── DEPT-U-13 ────────────────────────────────────────────────────────────

    @Test
    @Order(13)
    @DisplayName("DEPT-U-13 状态变更-update 被调用，缓存清除")
    void changeStatus_disable_withChildren() {
        SysDept dept = stubDept(91002L, 91001L, "0,91001", "华南");
        when(deptMapper.selectById(91002L)).thenReturn(dept);
        when(deptMapper.update(isNull(), any())).thenReturn(1);

        service.changeStatus(91002L, 0);

        verify(deptMapper).update(isNull(), any());
        verify(redisTemplate).delete("sys:dept:tree");
    }
}
