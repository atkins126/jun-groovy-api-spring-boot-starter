package com.gitthub.wujun728.engine;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gitthub.wujun728.engine.common.ApiService;
import com.gitthub.wujun728.engine.config.ApiPorperties;
import com.gitthub.wujun728.engine.groovy.cache.DefaultApiConfigCache;
import com.gitthub.wujun728.engine.groovy.cache.IApiConfigCache;
import com.gitthub.wujun728.engine.groovy.mapping.ApiProperties;
import com.gitthub.wujun728.engine.groovy.mapping.RequestMappingExecutor;
import com.gitthub.wujun728.engine.groovy.mapping.RequestMappingService;
import com.gitthub.wujun728.engine.util.JdbcUtil;
//import com.jun.plugin.engine.core.api.executor.GroovyDynamicLoader;

@Configuration
//@ConditionalOnProperty(prefix = "spring.groovy-api", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ApiPorperties.class)
public class ApiConfiguration {

	private final ApiPorperties dbConfig;

	public ApiConfiguration(ApiPorperties config) {
		this.dbConfig = config;
	}

	@Bean
	@ConditionalOnMissingBean(Api.class)
	public Api Engine() {
		return new Api();
	}

//	@Bean
//	@ConditionalOnMissingBean(GroovyDynamicLoader.class)
//	public GroovyDynamicLoader groovyDynamicLoader() {
//		return new GroovyDynamicLoader();
//	}

	@Bean
	@ConditionalOnMissingBean(ApiService.class)
	public ApiService apiService() {
		return new ApiService();
	}

	@Bean
	@ConditionalOnMissingBean(IApiConfigCache.class)
	public IApiConfigCache apiConfigCache() {
		return new DefaultApiConfigCache();
	}

	@Bean
	@ConditionalOnMissingBean(RequestMappingService.class)
	public RequestMappingService requestMappingService() {
		return new RequestMappingService();
	}

	@Bean
	@ConditionalOnMissingBean(ApiProperties.class)
	public ApiProperties apiProperties() {
		return new ApiProperties();
	}

	@Bean
	@ConditionalOnMissingBean(RequestMappingExecutor.class)
	public RequestMappingExecutor requestMappingExecutor() {
		return new RequestMappingExecutor();
	}
	
	@Bean
	@ConditionalOnMissingBean(JdbcUtil.class)
	public JdbcUtil jdbcUtil() {
		return new JdbcUtil();
	}

}