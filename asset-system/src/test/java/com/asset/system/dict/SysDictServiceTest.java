package com.asset.system.dict;

import com.asset.system.common.exception.SysBizException;
import com.asset.system.dict.dto.DictDataCreateDTO;
import com.asset.system.dict.dto.DictTypeCreateDTO;
import com.asset.system.dict.entity.SysDictData;
import com.asset.system.dict.entity.SysDictType;
import com.asset.system.dict.mapper.SysDictDataMapper;
import com.asset.system.dict.mapper.SysDictTypeMapper;
import com.asset.system.dict.service.impl.SysDictServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * §4.8 业务字典 — Service 单元测试
 * DICT-U-01 ~ DICT-U-08
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("§4.8 业务字典 Service 单元测试")
class SysDictServiceTest {

    @BeforeAll
    static void initMybatisLambdaCache() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        assistant.setCurrentNamespace("test_dict_ns");
        TableInfoHelper.initTableInfo(assistant, SysDictType.class);
        TableInfoHelper.initTableInfo(assistant, SysDictData.class);
    }

    @Mock SysDictTypeMapper     dictTypeMapper;
    @Mock SysDictDataMapper     dictDataMapper;
    @Mock StringRedisTemplate   redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks SysDictServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", dictTypeMapper);
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ─── 辅助 ─────────────────────────────────────────────────────────────────

    private SysDictData data(Long id, String dictType, String label, String value, Integer status) {
        SysDictData d = new SysDictData();
        d.setId(id);
        d.setDictType(dictType);
        d.setDictLabel(label);
        d.setDictValue(value);
        d.setStatus(status);
        d.setSortOrder(1);
        return d;
    }

    private SysDictType type(Long id, String dictType, String name) {
        SysDictType t = new SysDictType();
        t.setId(id);
        t.setDictType(dictType);
        t.setDictName(name);
        t.setStatus(1);
        return t;
    }

    // ─── DICT-U-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("DICT-U-01 新增字典类型：insert 被调用")
    void createType_success() {
        when(dictTypeMapper.selectCount(any())).thenReturn(0L);
        when(dictTypeMapper.insert(any(SysDictType.class))).thenReturn(1);

        DictTypeCreateDTO dto = new DictTypeCreateDTO();
        dto.setDictType("new_type");
        dto.setDictName("新类型");

        service.createType(dto);

        verify(dictTypeMapper).insert(any(SysDictType.class));
    }

    // ─── DICT-U-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("DICT-U-02 新增-类型编码重复：抛出编码已存在异常")
    void createType_duplicate_throws() {
        when(dictTypeMapper.selectCount(any())).thenReturn(1L);

        DictTypeCreateDTO dto = new DictTypeCreateDTO();
        dto.setDictType("test_project_status");
        dto.setDictName("项目状态副本");

        assertThatThrownBy(() -> service.createType(dto))
                .isInstanceOf(SysBizException.class)
                .hasMessageContaining("已存在");
    }

    // ─── DICT-U-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("DICT-U-03 删除字典类型-级联删数据+缓存驱逐")
    void deleteType_cascadeDeleteDataAndCache() {
        when(dictTypeMapper.selectById(91001L)).thenReturn(type(91001L, "test_project_status", "项目状态"));
        when(dictTypeMapper.deleteById(91001L)).thenReturn(1);
        when(dictDataMapper.logicDeleteByDictType("test_project_status")).thenReturn(3);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        service.deleteType(91001L);

        verify(dictDataMapper).logicDeleteByDictType("test_project_status");
        verify(redisTemplate).delete("sys:dict:test_project_status");
    }

    // ─── DICT-U-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("DICT-U-04 查询字典数据-命中缓存：不查DB")
    void listData_hitCache_returnFromRedis() throws Exception {
        List<SysDictData> cached = List.of(
                data(91001L, "test_project_status", "筹备中", "1", 1),
                data(91002L, "test_project_status", "运营中", "2", 1));
        String json = objectMapper.writeValueAsString(cached);
        when(valueOps.get("sys:dict:test_project_status")).thenReturn(json);

        List<SysDictData> result = service.listData("test_project_status");

        assertThat(result).hasSize(2);
        verify(dictDataMapper, never()).selectByDictType(any());
    }

    // ─── DICT-U-05 ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("DICT-U-05 查询字典数据-未命中缓存：查DB并写入Redis(TTL=60min)")
    void listData_missCache_queryDbAndCache() {
        when(valueOps.get("sys:dict:test_project_status")).thenReturn(null);
        when(dictDataMapper.selectByDictType("test_project_status")).thenReturn(
                List.of(data(91001L, "test_project_status", "筹备中", "1", 1)));

        List<SysDictData> result = service.listData("test_project_status");

        assertThat(result).hasSize(1);
        verify(dictDataMapper).selectByDictType("test_project_status");
        verify(valueOps).set(eq("sys:dict:test_project_status"), anyString(), eq(60L), eq(TimeUnit.MINUTES));
    }

    // ─── DICT-U-06 ────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("DICT-U-06 新增字典数据-刷新缓存：缓存被驱逐")
    void createData_refreshCache() {
        when(dictDataMapper.insert(any(SysDictData.class))).thenReturn(1);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        DictDataCreateDTO dto = new DictDataCreateDTO();
        dto.setDictType("test_project_status");
        dto.setDictLabel("停业整顿");
        dto.setDictValue("4");

        service.createData(dto);

        verify(dictDataMapper).insert(any(SysDictData.class));
        verify(redisTemplate).delete("sys:dict:test_project_status");
    }

    // ─── DICT-U-07 ────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("DICT-U-07 编辑字典数据-刷新缓存：updateById + 缓存驱逐")
    void updateData_refreshCache() {
        when(dictDataMapper.selectById(91001L))
                .thenReturn(data(91001L, "test_project_status", "筹备中", "1", 1));
        when(dictDataMapper.updateById(any(SysDictData.class))).thenReturn(1);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        DictDataCreateDTO dto = new DictDataCreateDTO();
        dto.setId(91001L);
        dto.setDictLabel("筹备中（已修改）");
        dto.setDictValue("1");

        service.updateData(dto);

        verify(dictDataMapper).updateById(argThat((SysDictData d) -> d.getId().equals(91001L)));
        verify(redisTemplate).delete("sys:dict:test_project_status");
    }

    // ─── DICT-U-08 ────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("DICT-U-08 全部数据查询-含停用：返回结果含 status=0 数据")
    void listAllData_includesDisabled() {
        when(dictDataMapper.selectAllByDictType("test_project_status")).thenReturn(List.of(
                data(91001L, "test_project_status", "筹备中", "1", 1),
                data(91002L, "test_project_status", "运营中", "2", 1),
                data(91099L, "test_project_status", "停用项", "9", 0)));

        List<SysDictData> result = service.listAllData("test_project_status");

        assertThat(result).hasSize(3);
        assertThat(result).anyMatch(d -> d.getStatus() == 0);
        verify(dictDataMapper).selectAllByDictType("test_project_status");
    }
}
