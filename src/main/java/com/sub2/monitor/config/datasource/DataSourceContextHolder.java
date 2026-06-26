package com.sub2.monitor.config.datasource;

public final class DataSourceContextHolder {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private DataSourceContextHolder() {
    }

    public static void use(String dataSourceName) {
        CONTEXT.set(dataSourceName);
    }

    public static String get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
