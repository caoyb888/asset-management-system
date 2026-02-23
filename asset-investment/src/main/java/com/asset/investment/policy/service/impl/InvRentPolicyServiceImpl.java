package com.asset.investment.policy.service.impl;

import com.asset.investment.policy.entity.InvRentPolicy;
import com.asset.investment.policy.mapper.InvRentPolicyMapper;
import com.asset.investment.policy.service.InvRentPolicyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class InvRentPolicyServiceImpl extends ServiceImpl<InvRentPolicyMapper, InvRentPolicy> implements InvRentPolicyService {}
