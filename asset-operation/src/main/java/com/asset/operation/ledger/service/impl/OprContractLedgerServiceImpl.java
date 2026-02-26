package com.asset.operation.ledger.service.impl;

import com.asset.operation.ledger.entity.OprContractLedger;
import com.asset.operation.ledger.mapper.OprContractLedgerMapper;
import com.asset.operation.ledger.service.OprContractLedgerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** OprContractLedger ServiceImpl（桩，后续各阶段补充业务逻辑） */
@Slf4j
@Service
public class OprContractLedgerServiceImpl extends ServiceImpl<OprContractLedgerMapper, OprContractLedger>
        implements OprContractLedgerService {}
