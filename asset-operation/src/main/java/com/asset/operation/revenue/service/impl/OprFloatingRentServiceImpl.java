package com.asset.operation.revenue.service.impl;

import com.asset.operation.revenue.entity.OprFloatingRent;
import com.asset.operation.revenue.mapper.OprFloatingRentMapper;
import com.asset.operation.revenue.service.OprFloatingRentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** OprFloatingRent ServiceImpl（桩，后续各阶段补充业务逻辑） */
@Slf4j
@Service
public class OprFloatingRentServiceImpl extends ServiceImpl<OprFloatingRentMapper, OprFloatingRent>
        implements OprFloatingRentService {}
