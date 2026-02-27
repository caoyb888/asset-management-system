package com.asset.finance.receipt.service.impl;
import com.asset.finance.receipt.entity.FinReceipt;
import com.asset.finance.receipt.mapper.FinReceiptMapper;
import com.asset.finance.receipt.service.FinReceiptService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
@Service public class FinReceiptServiceImpl extends ServiceImpl<FinReceiptMapper, FinReceipt> implements FinReceiptService {}
