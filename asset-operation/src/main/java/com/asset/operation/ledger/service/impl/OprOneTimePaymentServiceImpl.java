package com.asset.operation.ledger.service.impl;

import com.asset.operation.ledger.entity.OprOneTimePayment;
import com.asset.operation.ledger.mapper.OprOneTimePaymentMapper;
import com.asset.operation.ledger.service.OprOneTimePaymentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** OprOneTimePayment ServiceImpl（桩，后续各阶段补充业务逻辑） */
@Slf4j
@Service
public class OprOneTimePaymentServiceImpl extends ServiceImpl<OprOneTimePaymentMapper, OprOneTimePayment>
        implements OprOneTimePaymentService {}
