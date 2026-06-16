package com.sub2.monitor.config.datasource;

/**
 * 动态数据源上下文。
 *
 * <p>通过 ThreadLocal 保存当前线程要使用的数据源。Spring 的
 * {@link org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource}
 * 在获取连接时会读取该上下文，从而决定本次数据库操作走哪个真实 DataSource。</p>
 *
 * <p>使用时必须遵循 try/finally 模式，确保业务执行完成后清理上下文，避免线程池复用
 * 导致后续请求误用上一次的数据源。</p>
 */
public final class DynamicDataSourceContextHolder {

    private static final ThreadLocal<DataSourceKey> CONTEXT = new ThreadLocal<>();

    private DynamicDataSourceContextHolder() {
    }

    /**
     * 设置当前线程的数据源。
     */
    public static void set(DataSourceKey dataSourceKey) {
        CONTEXT.set(dataSourceKey);
    }

    /**
     * 获取当前线程的数据源；为空时由路由数据源回退到默认主库。
     */
    public static DataSourceKey get() {
        return CONTEXT.get();
    }

    /**
     * 清理当前线程的数据源标识。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
