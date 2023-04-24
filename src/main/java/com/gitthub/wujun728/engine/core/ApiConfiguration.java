package com.gitthub.wujun728.engine.core;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gitthub.wujun728.engine.core.config.ApiPorperties;

@Configuration
@ConditionalOnProperty(prefix = "spring.groovy-api", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ApiPorperties.class)
public class ApiConfiguration {

    private final ApiPorperties dbConfig;

    public ApiConfiguration(ApiPorperties config) {
        this.dbConfig = config;
    }

    @Bean
    @ConditionalOnMissingBean(Api.class)
    public Api Engine(){
        return new Api();
    }
}