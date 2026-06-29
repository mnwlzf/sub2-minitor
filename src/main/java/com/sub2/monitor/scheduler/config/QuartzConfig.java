package com.sub2.monitor.scheduler.config;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class QuartzConfig {

    @Bean
    public AutowiringSpringBeanJobFactory jobFactory(AutowireCapableBeanFactory beanFactory) {
        return new AutowiringSpringBeanJobFactory(beanFactory);
    }

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(AutowiringSpringBeanJobFactory jobFactory) {
        return new SchedulerFactoryBeanCustomizer(jobFactory);
    }

    public static class SchedulerFactoryBeanCustomizer implements org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer {

        private final AutowiringSpringBeanJobFactory jobFactory;

        public SchedulerFactoryBeanCustomizer(AutowiringSpringBeanJobFactory jobFactory) {
            this.jobFactory = jobFactory;
        }

        @Override
        public void customize(SchedulerFactoryBean schedulerFactoryBean) {
            schedulerFactoryBean.setJobFactory(jobFactory);
        }
    }
}
