package com.asset.investment.contract.service.impl;

import com.asset.investment.contract.entity.InvLeaseContractVersion;
import com.asset.investment.contract.mapper.InvLeaseContractVersionMapper;
import com.asset.investment.contract.service.InvLeaseContractVersionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class InvLeaseContractVersionServiceImpl extends ServiceImpl<InvLeaseContractVersionMapper, InvLeaseContractVersion> implements InvLeaseContractVersionService {}
