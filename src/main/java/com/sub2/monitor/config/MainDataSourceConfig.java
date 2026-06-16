package com.sub2.monitor.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.sub2.monitor.config.datasource.DataSourceKey;
import com.sub2.monitor.config.datasource.DynamicRoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * MyBatis-Plus dynamic datasource configuration.
 *
 * <p>Spring and MyBatis-Plus see only one primary DataSource: {@link DynamicRoutingDataSource}.
 * The routing datasource holds the real main datasource and sub2api datasource. Business code
 * explicitly switches the current thread datasource through DataSourceSwitcher.</p>
 *
 * <p>All mappers share one SqlSessionFactory. The executed database is decided at connection
 * acquisition time by {@link ()}.</p>
 */
@Configuration
@MapperScan(basePackages = "com.sub2.monitor.mapper", sqlSessionFactoryRef = "sqlSessionFactory")
@Slf4j
public class MainDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("app.datasource.main")
    public DataSourceProperties mainDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Real main business datasource.
     */
    @Bean
    @QuartzDataSource
    @ConfigurationProperties("app.datasource.main.hikari")
    public DataSource mainDataSource(@Qualifier("mainDataSourceProperties") DataSourceProperties mainDataSourceProperties) {
        log.info("开始初始化主数据源, url={}, username={}",
                mainDataSourceProperties.getUrl(),
                mainDataSourceProperties.getUsername());
        return mainDataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * Primary datasource exposed to Spring.
     *
     * <p>Default target is main datasource. When the current thread context is SUB2_API,
     * SQL execution is routed to sub2ApiDataSource.</p>
     */
    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("mainDataSource") DataSource mainDataSource,
                                 @Qualifier("sub2ApiDataSource") DataSource sub2ApiDataSource) {
        log.info("开始组装路由数据源, defaultTarget=MAIN, targets=[MAIN,SUB2_API]");
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceKey.MAIN, mainDataSource);
        targetDataSources.put(DataSourceKey.SUB2_API, sub2ApiDataSource);

        DynamicRoutingDataSource routingDataSource = new DynamicRoutingDataSource();
        routingDataSource.setDefaultTargetDataSource(mainDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.afterPropertiesSet();
        log.info("路由数据源初始化完成");
        return routingDataSource;
    }

    /**
     * MyBatis-Plus SqlSessionFactory using the routing datasource.
     *
     * <p>Both main mapper XML and sub2api mapper XML are loaded here. Explicit datasource
     * switching decides which real database receives each SQL.</p>
     */
    @Bean
    @Primary
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/**/*.xml"));
        return factoryBean.getObject();
    }
}
