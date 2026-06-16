package com.sub2.monitor.config;

import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import javax.sql.DataSource;

@Configuration
public class QuartzConfig {

    @Bean
    public JobFactory jobFactory(AutowireCapableBeanFactory beanFactory) {
        return new SpringBeanJobFactory() {
            @Override
            protected Object createJobInstance(org.quartz.spi.TriggerFiredBundle bundle) throws Exception {
                Object job = super.createJobInstance(bundle);
                beanFactory.autowireBean(job);
                return job;
            }
        };
    }

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(@Qualifier("mainDataSource") DataSource dataSource,
                                                                          JobFactory jobFactory) {
        return schedulerFactoryBean -> {
            schedulerFactoryBean.setDataSource(dataSource);
            schedulerFactoryBean.setJobFactory(jobFactory);
            schedulerFactoryBean.setWaitForJobsToCompleteOnShutdown(true);
            schedulerFactoryBean.setOverwriteExistingJobs(true);
        };
    }
}
