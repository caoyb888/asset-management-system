package com.asset.finance.voucher.service.impl;
import com.asset.finance.voucher.entity.FinVoucher;
import com.asset.finance.voucher.mapper.FinVoucherMapper;
import com.asset.finance.voucher.service.FinVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
@Service public class FinVoucherServiceImpl extends ServiceImpl<FinVoucherMapper, FinVoucher> implements FinVoucherService {}
