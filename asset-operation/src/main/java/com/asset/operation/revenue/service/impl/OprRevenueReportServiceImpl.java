package com.asset.operation.revenue.service.impl;

import com.asset.operation.revenue.entity.OprRevenueReport;
import com.asset.operation.revenue.mapper.OprRevenueReportMapper;
import com.asset.operation.revenue.service.OprRevenueReportService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** OprRevenueReport ServiceImpl（桩，后续各阶段补充业务逻辑） */
@Slf4j
@Service
public class OprRevenueReportServiceImpl extends ServiceImpl<OprRevenueReportMapper, OprRevenueReport>
        implements OprRevenueReportService {}
