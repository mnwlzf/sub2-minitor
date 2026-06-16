package com.sub2.monitor.config.datasource;

import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 显式数据源切换工具。
 *
 * <p>业务代码通过 {@code dataSourceSwitcher.use(DataSourceKey.SUB2_API, () -> mapper.xxx())}
 * 明确声明接下来的数据库操作使用哪个库。该工具内部统一负责 set/clear，避免遗漏清理。</p>
 */
@Component
public class DataSourceSwitcher {

    /**
     * 在指定数据源下执行有返回值的操作。
     */
    public <T> T use(DataSourceKey dataSourceKey, Supplier<T> supplier) {
        DynamicDataSourceContextHolder.set(dataSourceKey);
        try {
            return supplier.get();
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    /**
     * 在指定数据源下执行无返回值的操作。
     */
    public void use(DataSourceKey dataSourceKey, Runnable runnable) {
        DynamicDataSourceContextHolder.set(dataSourceKey);
        try {
            runnable.run();
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }
}
