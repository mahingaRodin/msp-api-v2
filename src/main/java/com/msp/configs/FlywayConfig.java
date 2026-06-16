package com.msp.configs;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures Flyway finishes before Hibernate schema validation in prod/k8s.
 */
@Configuration
public class FlywayConfig implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory.containsBean("entityManagerFactory")
                && beanFactory.containsBeanDefinition("flywayInitializer")) {
            beanFactory.getBeanDefinition("entityManagerFactory").setDependsOn("flywayInitializer");
        }
    }
}
