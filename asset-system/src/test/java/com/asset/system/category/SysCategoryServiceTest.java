package com.asset.system.category;

import com.asset.system.category.dto.CategoryCreateDTO;
import com.asset.system.category.dto.CategoryQueryDTO;
import com.asset.system.category.dto.CategoryTreeVO;
import com.asset.system.category.entity.SysCategory;
import com.asset.system.category.mapper.SysCategoryMapper;
import com.asset.system.category.service.impl.SysCategoryServiceImpl;
import com.asset.system.common.exception.SysBizException;
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
 * §4.11 分类管理 — Service 单元测试
 * CAT-U-01 ~ CAT-U-05
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("§4.11 分类管理 Service 单元测试")
class SysCategoryServiceTest {

    @BeforeAll
    static void initMybatisLambdaCache() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        assistant.setCurrentNamespace("test_category_ns");
        TableInfoHelper.initTableInfo(assistant, SysCategory.class);
    }

    @Mock SysCategoryMapper categoryMapper;

    @InjectMocks SysCategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", categoryMapper);
    }

    // ─── 辅助 ─────────────────────────────────────────────────────────────────

    private SysCategory cat(Long id, Long parentId, String type, String code, String name, int level, String ancestors) {
        SysCategory c = new SysCategory();
        c.setId(id);
        c.setParentId(parentId);
        c.setCategoryType(type);
        c.setCategoryCode(code);
        c.setCategoryName(name);
        c.setLevel(level);
        c.setAncestors(ancestors);
        c.setSortOrder(0);
        c.setStatus(1);
        return c;
    }

    // ─── CAT-U-01 ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("CAT-U-01 获取分类树-按维度过滤：根节点含 children")
    void getCategoryTree_byType_filtered() {
        SysCategory root  = cat(91001L, 0L,     "format", "TEST-FMT",    "业态分类", 1, "0");
        SysCategory child1 = cat(91002L, 91001L, "format", "TEST-FMT-CY", "餐饮",    2, "0,91001");
        SysCategory child2 = cat(91003L, 91001L, "format", "TEST-FMT-LS", "零售",    2, "0,91001");
        when(categoryMapper.selectList(any())).thenReturn(List.of(root, child1, child2));

        CategoryQueryDTO query = new CategoryQueryDTO();
        query.setCategoryType("format");

        List<CategoryTreeVO> tree = service.treeQuery(query);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getCategoryCode()).isEqualTo("TEST-FMT");
        assertThat(tree.get(0).getChildren()).hasSize(2);
    }

    // ─── CAT-U-02 ─────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("CAT-U-02 新增-编码重复：抛出编码已存在异常")
    void createCategory_duplicateCode_throws() {
        when(categoryMapper.selectCount(any())).thenReturn(1L);

        CategoryCreateDTO dto = new CategoryCreateDTO();
        dto.setCategoryType("format");
        dto.setCategoryCode("TEST-FMT-CY");
        dto.setCategoryName("餐饮副本");

        assertThatThrownBy(() -> service.createCategory(dto))
                .isInstanceOf(SysBizException.class)
                .hasMessageContaining("已存在");
    }

    // ─── CAT-U-03 ─────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("CAT-U-03 删除-有子分类：抛出存在子分类异常")
    void deleteCategory_hasChildren_throws() {
        when(categoryMapper.selectById(91001L))
                .thenReturn(cat(91001L, 0L, "format", "TEST-FMT", "业态分类", 1, "0"));
        when(categoryMapper.countChildren(91001L)).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteCategory(91001L))
                .isInstanceOf(SysBizException.class)
                .hasMessageContaining("子");
    }

    // ─── CAT-U-04 ─────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("CAT-U-04 删除-无子分类：removeById 被调用")
    void deleteCategory_noChildren_success() {
        when(categoryMapper.selectById(91002L))
                .thenReturn(cat(91002L, 91001L, "format", "TEST-FMT-CY", "餐饮", 2, "0,91001"));
        when(categoryMapper.countChildren(91002L)).thenReturn(0L);
        when(categoryMapper.deleteById(91002L)).thenReturn(1);

        service.deleteCategory(91002L);

        verify(categoryMapper).deleteById(91002L);
    }

    // ─── CAT-U-05 ─────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("CAT-U-05 获取所有分类维度：返回去重维度列表")
    void getTypes_returnsDistinctTypes() {
        // listCategoryTypes 通过 selectList + groupBy 取 categoryType
        SysCategory fmt  = new SysCategory(); fmt.setCategoryType("format");
        SysCategory area = new SysCategory(); area.setCategoryType("area");
        SysCategory type = new SysCategory(); type.setCategoryType("asset_type");
        when(categoryMapper.selectList(any())).thenReturn(List.of(fmt, area, type));

        List<String> types = service.listCategoryTypes();

        assertThat(types).containsExactlyInAnyOrder("format", "area", "asset_type");
    }
}
