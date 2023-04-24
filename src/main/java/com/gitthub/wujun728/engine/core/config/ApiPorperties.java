package com.gitthub.wujun728.engine.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("spring.groovy-api.config")
public class ApiPorperties {
    String sql;

    String datasource;


}
