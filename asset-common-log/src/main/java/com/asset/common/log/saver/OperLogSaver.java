package com.asset.common.log.saver;

/**
 * 操作日志持久化接口
 * <p>在 asset-system 中提供实现（写 sys_oper_log 表）；
 * 其他微服务未提供实现时 {@link org.springframework.beans.factory.annotation.Autowired}(required=false)，
 * 切面仅打印日志不写库。</p>
 */
public interface OperLogSaver {

    /**
     * 异步持久化操作日志
     *
     * @param record 由切面在主线程中采集好的日志记录
     */
    void save(OperLogRecord record);
}
