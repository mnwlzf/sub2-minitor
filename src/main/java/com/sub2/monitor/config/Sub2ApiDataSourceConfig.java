package com.sub2.monitor.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

/**
 * Real sub2api datasource configuration.
 *
 * <p>This class only creates the physical sub2api connection pool. It does not create a
 * dedicated SqlSessionFactory. The datasource is registered as a target of DynamicRoutingDataSource
 * in MainDataSourceConfig and is used only after explicit switching.</p>
 */
@Configuration
@Slf4j
public class Sub2ApiDataSourceConfig {

    @Bean
    @ConfigurationProperties("sub2api.datasource")
    public DataSourceProperties sub2ApiDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Real sub2api datasource.
     */
    @Bean
    @ConfigurationProperties("sub2api.datasource.hikari")
    public DataSource sub2ApiDataSource(@Qualifier("sub2ApiDataSourceProperties") DataSourceProperties sub2ApiDataSourceProperties) {
        log.info("开始初始化 sub2api 数据源, url={}, username={}",
                sub2ApiDataSourceProperties.getUrl(),
                sub2ApiDataSourceProperties.getUsername());
        HikariDataSource dataSource = sub2ApiDataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.setInitializationFailTimeout(0);
        log.info("sub2api 数据源基础参数已设置, poolName={}, maxPoolSize={}",
                dataSource.getPoolName(),
                dataSource.getMaximumPoolSize());
        return dataSource;
    }
}
