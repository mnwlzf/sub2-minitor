package com.sub2.monitor.config.datasource;

/**
 * 动态数据源标识。
 *
 * <p>所有需要显式切换数据源的位置都应该使用该枚举，避免在业务代码中散落字符串常量。</p>
 */
public enum DataSourceKey {
    /**
     * 默认主业务库，对应 application.yaml 中的 app.datasource.main。
     */
    MAIN,

    /**
     * sub2api 外部库，对应 application.yaml 中的 sub2api.datasource。
     */
    SUB2_API
}
