package com.asset.operation.flow.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.asset.common.exception.BizException;
import com.asset.operation.flow.dto.PassengerFlowCreateDTO;
import com.asset.operation.flow.dto.PassengerFlowQueryDTO;
import com.asset.operation.flow.dto.PassengerFlowStatisticsVO;
import com.asset.operation.flow.entity.OprPassengerFlow;
import com.asset.operation.flow.excel.PassengerFlowExcel;
import com.asset.operation.flow.mapper.OprPassengerFlowMapper;
import com.asset.operation.flow.service.OprPassengerFlowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 客流填报 ServiceImpl
 * 功能：CRUD + 批量导入 + Excel导出 + 日/周统计 + 30天趋势
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OprPassengerFlowServiceImpl extends ServiceImpl<OprPassengerFlowMapper, OprPassengerFlow>
        implements OprPassengerFlowService {

    private final JdbcTemplate jdbcTemplate;

    // =========================================================================
    // 查询
    // =========================================================================

    @Override
    public IPage<OprPassengerFlow> pageQuery(PassengerFlowQueryDTO query) {
        LambdaQueryWrapper<OprPassengerFlow> wrapper = new LambdaQueryWrapper<OprPassengerFlow>()
                .eq(query.getProjectId() != null, OprPassengerFlow::getProjectId, query.getProjectId())
                .eq(query.getBuildingId() != null, OprPassengerFlow::getBuildingId, query.getBuildingId())
                .eq(query.getFloorId() != null, OprPassengerFlow::getFloorId, query.getFloorId())
                .eq(query.getSourceType() != null, OprPassengerFlow::getSourceType, query.getSourceType())
                .ge(query.getStartDate() != null, OprPassengerFlow::getReportDate, query.getStartDate())
                .le(query.getEndDate() != null, OprPassengerFlow::getReportDate, query.getEndDate())
                .orderByDesc(OprPassengerFlow::getReportDate);

        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;
        IPage<OprPassengerFlow> result = page(new Page<>(pageNum, pageSize), wrapper);

        // 填充冗余名称
        result.getRecords().forEach(this::fillNames);
        return result;
    }

    // =========================================================================
    // 新增
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PassengerFlowCreateDTO dto) {
        validateRequired(dto);
        checkDuplicate(null, dto.getProjectId(), dto.getBuildingId(), dto.getFloorId(), dto.getReportDate());

        OprPassengerFlow entity = buildEntity(dto);
        entity.setSourceType(dto.getSourceType() != null ? dto.getSourceType() : 1); // 默认手动
        save(entity);
        log.info("[客流填报] 新增，id={}，date={}，count={}", entity.getId(), dto.getReportDate(), dto.getFlowCount());
        return entity.getId();
    }

    // =========================================================================
    // 编辑
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PassengerFlowCreateDTO dto) {
        OprPassengerFlow existing = getById(id);
        if (existing == null) throw new BizException("记录不存在，id=" + id);
        validateRequired(dto);
        checkDuplicate(id, dto.getProjectId(), dto.getBuildingId(), dto.getFloorId(), dto.getReportDate());

        existing.setProjectId(dto.getProjectId());
        existing.setBuildingId(dto.getBuildingId());
        existing.setFloorId(dto.getFloorId());
        existing.setReportDate(dto.getReportDate());
        existing.setFlowCount(dto.getFlowCount());
        updateById(existing);
        log.info("[客流填报] 编辑，id={}，date={}，count={}", id, dto.getReportDate(), dto.getFlowCount());
    }

    // =========================================================================
    // 删除
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        OprPassengerFlow existing = getById(id);
        if (existing == null) throw new BizException("记录不存在，id=" + id);
        removeById(id);
        log.info("[客流填报] 删除，id={}", id);
    }

    // =========================================================================
    // 批量导入
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(MultipartFile file) {
        List<String> errorList = new ArrayList<>();
        List<OprPassengerFlow> toSave = new ArrayList<>();
        final int[] rowIndex = {2};  // 从第2行开始（第1行是表头）

        try {
            EasyExcel.read(file.getInputStream(), PassengerFlowExcel.class,
                    new AnalysisEventListener<PassengerFlowExcel>() {
                        @Override
                        public void invoke(PassengerFlowExcel row, AnalysisContext ctx) {
                            int lineNum = rowIndex[0]++;
                            // 行级校验
                            if (row.getProjectCode() == null || row.getProjectCode().isBlank()) {
                                errorList.add("第" + lineNum + "行：项目编号不能为空");
                                return;
                            }
                            if (row.getReportDate() == null || row.getReportDate().isBlank()) {
                                errorList.add("第" + lineNum + "行：填报日期不能为空");
                                return;
                            }
                            if (row.getFlowCount() == null || row.getFlowCount().isBlank()) {
                                errorList.add("第" + lineNum + "行：客流人数不能为空");
                                return;
                            }

                            // 解析日期
                            LocalDate date;
                            try {
                                date = LocalDate.parse(row.getReportDate().trim());
                            } catch (Exception e) {
                                errorList.add("第" + lineNum + "行：日期格式错误，应为 YYYY-MM-DD");
                                return;
                            }

                            // 解析人数
                            int count;
                            try {
                                count = Integer.parseInt(row.getFlowCount().trim());
                                if (count < 0) throw new NumberFormatException();
                            } catch (Exception e) {
                                errorList.add("第" + lineNum + "行：客流人数必须为非负整数");
                                return;
                            }

                            // 查询项目ID
                            Long projectId = queryProjectIdByCode(row.getProjectCode().trim());
                            if (projectId == null) {
                                errorList.add("第" + lineNum + "行：项目编号【" + row.getProjectCode() + "】不存在");
                                return;
                            }

                            // 查询楼栋/楼层ID（选填）
                            Long buildingId = null;
                            Long floorId = null;
                            if (row.getBuildingCode() != null && !row.getBuildingCode().isBlank()) {
                                buildingId = queryBuildingIdByCode(projectId, row.getBuildingCode().trim());
                                if (buildingId == null) {
                                    errorList.add("第" + lineNum + "行：楼栋编号【" + row.getBuildingCode() + "】不存在");
                                    return;
                                }
                            }
                            if (row.getFloorCode() != null && !row.getFloorCode().isBlank() && buildingId != null) {
                                floorId = queryFloorIdByCode(buildingId, row.getFloorCode().trim());
                                if (floorId == null) {
                                    errorList.add("第" + lineNum + "行：楼层编号【" + row.getFloorCode() + "】不存在");
                                    return;
                                }
                            }

                            OprPassengerFlow entity = new OprPassengerFlow();
                            entity.setProjectId(projectId);
                            entity.setBuildingId(buildingId);
                            entity.setFloorId(floorId);
                            entity.setReportDate(date);
                            entity.setFlowCount(count);
                            entity.setSourceType(2);  // 导入
                            toSave.add(entity);
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext ctx) {}
                    }).sheet().doRead();
        } catch (IOException e) {
            throw new BizException("读取 Excel 文件失败：" + e.getMessage());
        }

        // 批量保存（对于重复的日期做 upsert：更新 flow_count）
        int successCount = 0;
        for (OprPassengerFlow entity : toSave) {
            try {
                // 检查唯一键冲突
                Long existId = findExistingId(entity.getProjectId(), entity.getBuildingId(),
                        entity.getFloorId(), entity.getReportDate());
                if (existId != null) {
                    // 已存在则更新
                    entity.setId(existId);
                    updateById(entity);
                } else {
                    save(entity);
                }
                successCount++;
            } catch (Exception e) {
                log.warn("[客流导入] 保存失败：{}", e.getMessage());
                errorList.add("第 ? 行：数据保存失败 - " + e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", errorList.size());
        result.put("errorList", errorList);
        log.info("[客流导入] 完成，成功{}条，失败{}条", successCount, errorList.size());
        return result;
    }

    // =========================================================================
    // 导出
    // =========================================================================

    @Override
    public void exportExcel(PassengerFlowQueryDTO query, HttpServletResponse response) {
        // 查全量数据（不分页）
        query.setPageNum(1);
        query.setPageSize(Integer.MAX_VALUE);
        IPage<OprPassengerFlow> page = pageQuery(query);

        List<PassengerFlowExcel> rows = page.getRecords().stream().map(e -> {
            PassengerFlowExcel row = new PassengerFlowExcel();
            row.setProjectCode(getProjectCode(e.getProjectId()));
            row.setBuildingCode(getBuildingCode(e.getBuildingId()));
            row.setFloorCode(getFloorCode(e.getFloorId()));
            row.setReportDate(e.getReportDate() != null ? e.getReportDate().toString() : "");
            row.setFlowCount(e.getFlowCount() != null ? String.valueOf(e.getFlowCount()) : "0");
            return row;
        }).collect(Collectors.toList());

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode("客流填报数据_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), PassengerFlowExcel.class)
                    .sheet("客流数据").doWrite(rows);
        } catch (IOException e) {
            throw new BizException("导出 Excel 失败：" + e.getMessage());
        }
    }

    // =========================================================================
    // 统计分析
    // =========================================================================

    @Override
    public PassengerFlowStatisticsVO statistics(Long projectId, Long buildingId, Long floorId) {
        PassengerFlowStatisticsVO vo = new PassengerFlowStatisticsVO();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 今日/昨日客流（汇总同维度所有记录）
        int todayFlow = sumFlow(projectId, buildingId, floorId, today, today);
        int yesterdayFlow = sumFlow(projectId, buildingId, floorId, yesterday, yesterday);
        vo.setTodayFlow(todayFlow);
        vo.setYesterdayFlow(yesterdayFlow);
        vo.setDayOverDayRate(calcRate(todayFlow, yesterdayFlow));

        // 本周/上周
        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate thisWeekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = thisWeekStart.minusDays(1);

        int thisWeekFlow = sumFlow(projectId, buildingId, floorId, thisWeekStart, thisWeekEnd);
        int lastWeekFlow = sumFlow(projectId, buildingId, floorId, lastWeekStart, lastWeekEnd);
        vo.setThisWeekFlow(thisWeekFlow);
        vo.setLastWeekFlow(lastWeekFlow);
        vo.setWeekOverWeekRate(calcRate(thisWeekFlow, lastWeekFlow));

        // 近30天
        LocalDate thirtyDaysAgo = today.minusDays(29);
        int last30Days = sumFlow(projectId, buildingId, floorId, thirtyDaysAgo, today);
        vo.setLast30DaysFlow(last30Days);

        // 近30天趋势（按日聚合）
        List<PassengerFlowStatisticsVO.DailyPoint> trendPoints = buildTrendPoints(
                projectId, buildingId, floorId, thirtyDaysAgo, today);
        vo.setTrendPoints(trendPoints);

        return vo;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 私有辅助
    // ─────────────────────────────────────────────────────────────────────────

    /** 汇总指定维度和日期区间的客流总量 */
    private int sumFlow(Long projectId, Long buildingId, Long floorId,
                        LocalDate start, LocalDate end) {
        LambdaQueryWrapper<OprPassengerFlow> w = new LambdaQueryWrapper<OprPassengerFlow>()
                .eq(projectId != null, OprPassengerFlow::getProjectId, projectId)
                .eq(buildingId != null, OprPassengerFlow::getBuildingId, buildingId)
                .eq(floorId != null, OprPassengerFlow::getFloorId, floorId)
                .ge(OprPassengerFlow::getReportDate, start)
                .le(OprPassengerFlow::getReportDate, end);
        List<OprPassengerFlow> records = list(w);
        return records.stream().mapToInt(r -> r.getFlowCount() != null ? r.getFlowCount() : 0).sum();
    }

    /** 构建每日趋势列表（填充0值） */
    private List<PassengerFlowStatisticsVO.DailyPoint> buildTrendPoints(
            Long projectId, Long buildingId, Long floorId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<OprPassengerFlow> w = new LambdaQueryWrapper<OprPassengerFlow>()
                .eq(projectId != null, OprPassengerFlow::getProjectId, projectId)
                .eq(buildingId != null, OprPassengerFlow::getBuildingId, buildingId)
                .eq(floorId != null, OprPassengerFlow::getFloorId, floorId)
                .ge(OprPassengerFlow::getReportDate, start)
                .le(OprPassengerFlow::getReportDate, end)
                .orderByAsc(OprPassengerFlow::getReportDate);

        // 按日期汇总
        Map<String, Integer> dayMap = new LinkedHashMap<>();
        list(w).forEach(r -> {
            String d = r.getReportDate().toString();
            dayMap.merge(d, r.getFlowCount() != null ? r.getFlowCount() : 0, Integer::sum);
        });

        // 补全区间内缺失日期（填0）
        List<PassengerFlowStatisticsVO.DailyPoint> points = new ArrayList<>();
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            String d = cur.toString();
            PassengerFlowStatisticsVO.DailyPoint pt = new PassengerFlowStatisticsVO.DailyPoint();
            pt.setDate(d);
            pt.setFlowCount(dayMap.getOrDefault(d, 0));
            points.add(pt);
            cur = cur.plusDays(1);
        }
        return points;
    }

    /** 计算环比增长率（%），base=0时返回null */
    private Double calcRate(int current, int base) {
        if (base == 0) return null;
        return BigDecimal.valueOf((double) (current - base) / base * 100)
                .setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** 校验必填字段 */
    private void validateRequired(PassengerFlowCreateDTO dto) {
        if (dto.getProjectId() == null) throw new BizException("项目ID不能为空");
        if (dto.getReportDate() == null) throw new BizException("填报日期不能为空");
        if (dto.getFlowCount() == null || dto.getFlowCount() < 0) throw new BizException("客流人数不能为空且不能为负数");
    }

    /** 唯一键校验（project + building + floor + date 不重复） */
    private void checkDuplicate(Long excludeId, Long projectId, Long buildingId, Long floorId, LocalDate reportDate) {
        Long existId = findExistingId(projectId, buildingId, floorId, reportDate);
        if (existId != null && !existId.equals(excludeId)) {
            throw new BizException("该维度（项目/楼栋/楼层）该日期已存在客流记录，请勿重复填报");
        }
    }

    private Long findExistingId(Long projectId, Long buildingId, Long floorId, LocalDate reportDate) {
        LambdaQueryWrapper<OprPassengerFlow> w = new LambdaQueryWrapper<OprPassengerFlow>()
                .eq(OprPassengerFlow::getProjectId, projectId)
                .eq(OprPassengerFlow::getReportDate, reportDate)
                .eq(buildingId != null, OprPassengerFlow::getBuildingId, buildingId)
                .isNull(buildingId == null, OprPassengerFlow::getBuildingId)
                .eq(floorId != null, OprPassengerFlow::getFloorId, floorId)
                .isNull(floorId == null, OprPassengerFlow::getFloorId)
                .last("LIMIT 1");
        OprPassengerFlow existing = getOne(w);
        return existing != null ? existing.getId() : null;
    }

    private OprPassengerFlow buildEntity(PassengerFlowCreateDTO dto) {
        OprPassengerFlow entity = new OprPassengerFlow();
        entity.setProjectId(dto.getProjectId());
        entity.setBuildingId(dto.getBuildingId());
        entity.setFloorId(dto.getFloorId());
        entity.setReportDate(dto.getReportDate());
        entity.setFlowCount(dto.getFlowCount());
        return entity;
    }

    /** 填充冗余名称（项目名/楼栋名/楼层名，查不到则不填） */
    private void fillNames(OprPassengerFlow e) {
        // 名称字段不在实体中，前端通过 ID 映射展示
        // 此处预留钩子，若实体扩展了 transient 字段则在此填充
    }

    // 下面是 Excel 导入时的辅助查询

    private Long queryProjectIdByCode(String code) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM biz_project WHERE project_code=? AND is_deleted=0 LIMIT 1",
                    Long.class, code);
        } catch (Exception e) { return null; }
    }

    private Long queryBuildingIdByCode(Long projectId, String code) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM biz_building WHERE project_id=? AND building_code=? AND is_deleted=0 LIMIT 1",
                    Long.class, projectId, code);
        } catch (Exception e) { return null; }
    }

    private Long queryFloorIdByCode(Long buildingId, String code) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM biz_floor WHERE building_id=? AND floor_code=? AND is_deleted=0 LIMIT 1",
                    Long.class, buildingId, code);
        } catch (Exception e) { return null; }
    }

    private String getProjectCode(Long id) {
        if (id == null) return "";
        try {
            return jdbcTemplate.queryForObject("SELECT project_code FROM biz_project WHERE id=? LIMIT 1",
                    String.class, id);
        } catch (Exception e) { return ""; }
    }

    private String getBuildingCode(Long id) {
        if (id == null) return "";
        try {
            return jdbcTemplate.queryForObject("SELECT building_code FROM biz_building WHERE id=? LIMIT 1",
                    String.class, id);
        } catch (Exception e) { return ""; }
    }

    private String getFloorCode(Long id) {
        if (id == null) return "";
        try {
            return jdbcTemplate.queryForObject("SELECT floor_code FROM biz_floor WHERE id=? LIMIT 1",
                    String.class, id);
        } catch (Exception e) { return ""; }
    }
}
