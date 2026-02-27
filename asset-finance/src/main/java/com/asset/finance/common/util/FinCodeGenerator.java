package com.asset.finance.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 财务模块单号生成器
 *
 * <p>生成格式：{前缀}-{yyyyMMdd}-{6位序号}，示例：RC-20260201-000001
 *
 * <p>实现策略：本地 AtomicLong 计数，每日重置（按日期前缀区分）。
 * 适用于单实例部署；多实例部署时可替换为 Redis INCR 实现（见注释）。
 *
 * <p>前缀约定：
 * <ul>
 *   <li>RC — 收款单</li>
 *   <li>WO — 核销单</li>
 *   <li>AR — 应收编号</li>
 *   <li>DD — 减免单</li>
 *   <li>AJ — 调整单</li>
 *   <li>VC — 凭证号</li>
 * </ul>
 */
@Slf4j
@Component
public class FinCodeGenerator {

    public static final String PREFIX_RECEIPT     = "RC";
    public static final String PREFIX_WRITE_OFF   = "WO";
    public static final String PREFIX_RECEIVABLE  = "AR";
    public static final String PREFIX_DEDUCTION   = "DD";
    public static final String PREFIX_ADJUSTMENT  = "AJ";
    public static final String PREFIX_VOUCHER     = "VC";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** key = "前缀-日期"，value = 当日序号计数器 */
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    /**
     * 生成单号
     *
     * @param prefix 业务前缀，使用本类 PREFIX_* 常量
     * @return 格式化单号，如 RC-20260201-000001
     */
    public String generate(String prefix) {
        String date = LocalDate.now().format(DATE_FMT);
        String key = prefix + "-" + date;
        AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0));
        long seq = counter.incrementAndGet();
        String code = prefix + "-" + date + "-" + String.format("%06d", seq);
        log.debug("[单号生成] {}", code);
        return code;

        /*
         * Redis 分布式实现（多实例部署时替换以上逻辑）：
         *
         * String redisKey = "fin:code:" + prefix + ":" + date;
         * Long seq = redisTemplate.opsForValue().increment(redisKey);
         * redisTemplate.expire(redisKey, 2, TimeUnit.DAYS);  // 保留2天防跨日边界
         * return prefix + "-" + date + "-" + String.format("%06d", seq);
         */
    }

    /** 生成收款单号 */
    public String receiptCode()     { return generate(PREFIX_RECEIPT); }
    /** 生成核销单号 */
    public String writeOffCode()    { return generate(PREFIX_WRITE_OFF); }
    /** 生成应收编号 */
    public String receivableCode()  { return generate(PREFIX_RECEIVABLE); }
    /** 生成减免单号 */
    public String deductionCode()   { return generate(PREFIX_DEDUCTION); }
    /** 生成调整单号 */
    public String adjustmentCode()  { return generate(PREFIX_ADJUSTMENT); }
    /** 生成凭证号 */
    public String voucherCode()     { return generate(PREFIX_VOUCHER); }
}
