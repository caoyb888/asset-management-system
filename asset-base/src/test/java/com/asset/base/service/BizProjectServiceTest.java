package com.asset.base.service;

import com.asset.base.converter.ProjectConverter;
import com.asset.base.entity.BizProject;
import com.asset.base.entity.BizProjectContract;
import com.asset.base.mapper.BizProjectBankMapper;
import com.asset.base.mapper.BizProjectContractMapper;
import com.asset.base.mapper.BizProjectFinanceContactMapper;
import com.asset.base.mapper.BizProjectMapper;
import com.asset.base.model.dto.ProjectContractDTO;
import com.asset.base.model.dto.ProjectImageDTO;
import com.asset.base.model.dto.ProjectSaveDTO;
import com.asset.base.model.vo.ProjectVO;
import com.asset.base.service.impl.BizProjectServiceImpl;
import com.asset.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 项目管理 Service 单元测试（PROJ-U-01 ~ PROJ-U-11）
 * 使用 Mockito 纯 mock，不启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("项目管理 Service 单元测试")
class BizProjectServiceTest {

    @Mock
    BizProjectMapper projectMapper;

    @Mock
    ProjectConverter converter;

    @Mock
    BizProjectContractMapper contractMapper;

    @Mock
    BizProjectFinanceContactMapper financeContactMapper;

    @Mock
    BizProjectBankMapper bankMapper;

    @Spy
    @InjectMocks
    BizProjectServiceImpl projectService;

    @BeforeEach
    void setUp() {
        // 将 mock 的 Mapper 注入到 ServiceImpl 的 baseMapper 继承字段
        ReflectionTestUtils.setField(projectService, "baseMapper", projectMapper);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROJ-U-01 新增项目成功
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PROJ-U-01 新增项目-编号不存在-调用save成功")
    void createProject_uniqueCode_callsSave() {
        ProjectSaveDTO dto = new ProjectSaveDTO();
        dto.setProjectCode("P-NEW-01");
        dto.setProjectName("新项目");
        dto.setCompanyId(1L);

        BizProject entity = new BizProject();
        entity.setId(100L);

        when(converter.toEntity(dto)).thenReturn(entity);
        // 编号唯一校验：count 返回 0（不存在重复）
        doReturn(0L).when(projectService).count(any(LambdaQueryWrapper.class));
        // save 返回 true
        doReturn(true).when(projectService).save(entity);

        Long resultId = projectService.createProject(dto);

        assertThat(resultId).isEqualTo(100L);
        verify(projectService).save(entity);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROJ-U-02 新增-编号重复抛异常
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PROJ-U-02 新增项目-编号重复-抛出BizException")
    void createProject_duplicateCode_throwsBizException() {
        ProjectSaveDTO dto = new ProjectSaveDTO();
        dto.setProjectCode("P-EXIST");
        dto.setProjectName("重复编号项目");
        dto.setCompanyId(1L);

        // 唯一校验 count 返回 1（已存在）
        doReturn(1L).when(projectService).count(any(LambdaQueryWrapper.class));

        assertThatThrownBy(() -> projectService.createProject(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已存在");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROJ-U-03 编辑-编号未变不重复校验
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PROJ-U-03 编辑项目-编号未变更-跳过唯一性校验")
    void updateProject_sameCode_skipUniquenessCheck() {
        BizProject existing = new BizProject();
        existing.setId(1L);
        existing.setProjectCode("P-SAME");
        existing.setIsDeleted(0);

        ProjectSaveDTO dto = new ProjectSaveDTO();
        dto.setProjectCode("P-SAME"); // 编号未变

        doReturn(existing).when(projectService).getById(1L);
        doReturn(true).when(projectService).updateById(any());

        projectService.updateProject(1L, dto);

        // count（唯一性检查）不应被调用
        verify(projectService, never()).count(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROJ-U-04 编辑-新编号与已存在编号冲突
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PROJ-U-04 编辑项目-新编号冲突-抛出BizException")
    void updateProject_newCodeConflict_throwsBizException() {
        BizProject existing = new BizProject();
        existing.setId(1L);
        existing.setProjectCode("P-001");
        existing.setIsDeleted(0);

        ProjectSaveDTO dto = new ProjectSaveDTO();
        dto.setProjectCode("P-OTHER-EXIST"); // 与另一个项目冲突

        doReturn(existing).when(projectService).getById(1L);
        doReturn(1L).when(projectService).count(any(LambdaQueryWrapper.class));

        assertThatThrownBy(() -> projectService.updateProject(1L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已存在");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROJ-U-05 删除-项目不存在
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PROJ-U-05 删除项目-ID不存在-抛出BizException")
    void deleteProject_notFound_throwsBizException() {
        doReturn(null).when(projectService).getById(999999L);

        assertThatThrownBy(() -> projectService.deleteProject(999999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROJ-U-06 删除-已删除项目
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PROJ-U-06 删除项目-项目已删除-抛出BizException")
    void deleteProject_alreadyDeleted_throwsBizException() {
        BizProject deleted = new BizProject();
        deleted.setId(1L);
        deleted.setIsDeleted(1);

        doReturn(deleted).when(projectService).getById(1L);

        assertThatThrownBy(() -> projectService.deleteProject(1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROJ-U-07 枚举名称正确填充
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PROJ-U-07 枚举名称填充-全字段正确")
    void getProjectById_enumNamesFilledCorrectly() {
        BizProject project = new BizProject();
        project.setId(1L);
        project.setIsDeleted(0);

        ProjectVO vo = new ProjectVO();
        vo.setPropertyType(1);    // 国有
        vo.setBusinessType(2);    // 租赁
        vo.setOperationStatus(0); // 筹备

        doReturn(project).when(projectService).getById(1L);
        when(converter.toVO(project)).thenReturn(vo);

        ProjectVO result = projectService.getProjectById(1L);

        assertThat(result.getPropertyTypeName()).isEqualTo("国有");
        assertThat(result.getBusinessTypeName()).isEqualTo("租赁");
        assertThat(result.getOperationStatusName()).isEqualTo("筹备");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROJ-U-08 添加图片-imageUrls为空时初始化
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PROJ-U-08 添加图片-列表为null时自动初始化并设置sort=1")
    void addProjectImage_emptyList_initializesAndAdds() {
        BizProject project = new BizProject();
        project.setId(1L);
        project.setIsDeleted(0);
        project.setImageUrls(null); // 空列表

        doReturn(project).when(projectService).getById(1L);
        doReturn(true).when(projectService).updateById(any());

        ProjectImageDTO dto = new ProjectImageDTO();
        dto.setUrl("https://example.com/img.jpg");
        dto.setName("外观图");

        projectService.addProjectImage(1L, dto);

        // 验证 updateById 被调用，且图片列表已初始化
        verify(projectService).updateById(argThat(p -> {
            List<BizProject.ImageUrl> imgs = ((BizProject) p).getImageUrls();
            return imgs != null && imgs.size() == 1
                    && imgs.get(0).getSort() == 1
                    && "外观图".equals(imgs.get(0).getName());
        }));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROJ-U-09 删除图片-索引越界
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PROJ-U-09 删除图片-索引越界-抛出BizException")
    void deleteProjectImage_indexOutOfBound_throwsBizException() {
        BizProject.ImageUrl img1 = new BizProject.ImageUrl();
        img1.setUrl("https://a.com/1.jpg");
        BizProject.ImageUrl img2 = new BizProject.ImageUrl();
        img2.setUrl("https://a.com/2.jpg");

        BizProject project = new BizProject();
        project.setId(1L);
        project.setIsDeleted(0);
        project.setImageUrls(new ArrayList<>(List.of(img1, img2)));

        doReturn(project).when(projectService).getById(1L);

        // 索引 5 超出实际数量 2
        assertThatThrownBy(() -> projectService.deleteProjectImage(1L, 5))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("索引无效");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROJ-U-10 合同信息-不存在时新增
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PROJ-U-10 保存合同信息-无历史记录-调用insert")
    void saveContract_noExisting_callsInsert() {
        // 没有已有记录
        when(contractMapper.selectOne(any())).thenReturn(null);
        when(contractMapper.insert(any(BizProjectContract.class))).thenReturn(1);

        ProjectContractDTO dto = new ProjectContractDTO();
        dto.setPartyAName("测试甲方");

        projectService.saveContract(1L, dto);

        verify(contractMapper).insert(any(BizProjectContract.class));
        verify(contractMapper, never()).updateById(any(BizProjectContract.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROJ-U-11 合同信息-已存在时更新
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PROJ-U-11 保存合同信息-已有记录-调用updateById")
    void saveContract_existing_callsUpdate() {
        BizProjectContract existing = new BizProjectContract();
        existing.setId(1L);
        existing.setProjectId(1L);
        when(contractMapper.selectOne(any())).thenReturn(existing);
        when(contractMapper.updateById(any(BizProjectContract.class))).thenReturn(1);

        ProjectContractDTO dto = new ProjectContractDTO();
        dto.setPartyAName("更新后甲方名称");

        projectService.saveContract(1L, dto);

        verify(contractMapper).updateById(any(BizProjectContract.class));
        verify(contractMapper, never()).insert(any(BizProjectContract.class));
    }
}
