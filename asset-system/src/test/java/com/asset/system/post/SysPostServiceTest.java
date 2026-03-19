package com.asset.system.post;

import com.asset.system.common.exception.SysBizException;
import com.asset.system.post.dto.PostCreateDTO;
import com.asset.system.post.entity.SysPost;
import com.asset.system.post.mapper.SysPostMapper;
import com.asset.system.post.service.impl.SysPostServiceImpl;
import com.asset.system.user.mapper.SysUserMapper;
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
 * §4.4 岗位管理 — Service 单元测试
 * POST-U-01 ~ POST-U-06
 *
 * 不启动 Spring 容器，所有依赖通过 Mockito 注入。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("§4.4 岗位管理 Service 单元测试")
class SysPostServiceTest {

    @BeforeAll
    static void initMybatisLambdaCache() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        assistant.setCurrentNamespace("test_post_ns");
        TableInfoHelper.initTableInfo(assistant, SysPost.class);
    }

    @Mock SysPostMapper postMapper;
    @Mock SysUserMapper userMapper;

    @InjectMocks SysPostServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", postMapper);
    }

    // ─── 辅助 ─────────────────────────────────────────────────────────────────

    private SysPost stubPost(Long id, String code, String name, Integer status) {
        SysPost p = new SysPost();
        p.setId(id);
        p.setPostCode(code);
        p.setPostName(name);
        p.setStatus(status);
        p.setSortOrder(1);
        return p;
    }

    // ─── POST-U-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST-U-01 新增-编码唯一，insert 被调用")
    void createPost_uniqueCode_success() {
        when(postMapper.selectCount(any())).thenReturn(0L);
        when(postMapper.insert(any(SysPost.class))).thenReturn(1);

        PostCreateDTO dto = new PostCreateDTO();
        dto.setPostCode("NEW_POST");
        dto.setPostName("新岗位");

        service.createPost(dto);

        verify(postMapper).insert(any(SysPost.class));
    }

    // ─── POST-U-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("POST-U-02 新增-编码重复，抛出编码已存在异常")
    void createPost_duplicateCode_throws() {
        when(postMapper.selectCount(any())).thenReturn(1L);

        PostCreateDTO dto = new PostCreateDTO();
        dto.setPostCode("TEST_GM");
        dto.setPostName("总经理");

        assertThatThrownBy(() -> service.createPost(dto))
                .isInstanceOf(SysBizException.class)
                .hasMessageContaining("已存在");
    }

    // ─── POST-U-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("POST-U-03 删除-有关联用户，抛出存在关联用户异常")
    void deletePost_hasUsers_throws() {
        when(postMapper.selectById(91001L)).thenReturn(stubPost(91001L, "TEST_GM", "总经理", 1));
        when(userMapper.countByPostId(91001L)).thenReturn(1L);

        assertThatThrownBy(() -> service.deletePost(91001L))
                .isInstanceOf(SysBizException.class)
                .hasMessageContaining("用户");
    }

    // ─── POST-U-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("POST-U-04 删除-无关联，removeById 被调用")
    void deletePost_noUsers_success() {
        when(postMapper.selectById(91002L)).thenReturn(stubPost(91002L, "TEST_PM", "项目经理", 1));
        when(userMapper.countByPostId(91002L)).thenReturn(0L);
        when(postMapper.deleteById(91002L)).thenReturn(1);

        service.deletePost(91002L);

        verify(postMapper).deleteById(91002L);
    }

    // ─── POST-U-05 ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("POST-U-05 编辑-同编码不触发唯一性校验，update 被调用")
    void updatePost_sameCode_skipCheck() {
        // updatePost 不做编码唯一性校验，直接 update
        when(postMapper.selectById(91001L)).thenReturn(stubPost(91001L, "TEST_GM", "总经理", 1));
        when(postMapper.update(isNull(), any())).thenReturn(1);

        PostCreateDTO dto = new PostCreateDTO();
        dto.setId(91001L);
        dto.setPostCode("TEST_GM"); // 编码未变
        dto.setPostName("总经理（已更新）");

        assertThatNoException().isThrownBy(() -> service.updatePost(dto));
        verify(postMapper).update(isNull(), any());
    }

    // ─── POST-U-06 ────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("POST-U-06 下拉列表-只返回 status=1 的岗位")
    void listEnabled_onlyActivePost() {
        SysPost active = stubPost(91001L, "TEST_GM", "总经理", 1);
        when(postMapper.selectList(any())).thenReturn(List.of(active));

        List<SysPost> result = service.listEnabled();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(1);
    }
}
