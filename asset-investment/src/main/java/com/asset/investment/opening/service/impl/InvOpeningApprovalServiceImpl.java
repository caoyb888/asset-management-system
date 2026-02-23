package com.asset.investment.opening.service.impl;

import com.asset.investment.opening.entity.InvOpeningApproval;
import com.asset.investment.opening.mapper.InvOpeningApprovalMapper;
import com.asset.investment.opening.service.InvOpeningApprovalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class InvOpeningApprovalServiceImpl extends ServiceImpl<InvOpeningApprovalMapper, InvOpeningApproval> implements InvOpeningApprovalService {}
