package com.sub2.monitor.config.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态路由数据源。
 *
 * <p>MyBatis-Plus 只注入这一个 DataSource。每次执行 SQL 获取连接时，Spring 会调用
 * {@link #determineCurrentLookupKey()}，根据当前线程中的 {@link DataSourceKey}
 * 路由到主库或 sub2api 库。</p>
 */
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return DynamicDataSourceContextHolder.get();
    }
}
