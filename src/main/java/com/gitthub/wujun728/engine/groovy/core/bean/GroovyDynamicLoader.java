package com.gitthub.wujun728.engine.groovy.core.bean;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson2.JSON;
import com.gitthub.wujun728.engine.common.ApiConfig;
import com.gitthub.wujun728.engine.common.ApiService;
import com.gitthub.wujun728.engine.groovy.cache.IApiConfigCache;
import com.gitthub.wujun728.engine.groovy.core.cache.GroovyInfo;
import com.gitthub.wujun728.engine.groovy.core.cache.GroovyInnerCache;
import com.gitthub.wujun728.engine.groovy.mapping.RequestMappingService;

//import cn.hutool.core.lang.Console;
import groovy.lang.GroovyClassLoader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@Service
public class GroovyDynamicLoader implements ApplicationContextAware, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(GroovyDynamicLoader.class);

	private ConfigurableApplicationContext applicationContext;

	private BeanDefinitionRegistry registry;
//    @Autowired
//    GroovyClassLoader groovyClassLoader;

	private static final GroovyClassLoader groovyClassLoader = new GroovyClassLoader(
			GroovyDynamicLoader.class.getClassLoader());

//	@Resource
//	private IGroovyScriptService groovyScriptService;

	@Autowired
	private ApiService apiService;

	@Autowired
	private IApiConfigCache apiInfoCache;

	@Autowired
	private RequestMappingService requestMappingService;

	@Override
	public void afterPropertiesSet() throws Exception {

		long start = System.currentTimeMillis();
		System.out.println("开始解析groovy脚本...");

		logger.trace(" --- trace --- ");
		logger.debug(" --- debug --- ");
		logger.info(" --- info --- ");
		logger.warn(" --- warn --- ");
		logger.error(" --- error --- ");

//		init();
		initNew();

		long cost = System.currentTimeMillis() - start;
		System.out.println("结束解析groovy脚本...，耗时：" + cost);
	}

	private void initNew() {
		List<ApiConfig> groovyScripts = apiService.queryApiConfigList();

		apiInfoCache.putAll(groovyScripts);

		List<GroovyInfo> groovyInfos = convert(groovyScripts);

		initNew(groovyInfos);

		refreshMapping(groovyScripts);
	}

	@SuppressWarnings("rawtypes")
	private void initNew(List<GroovyInfo> groovyInfos) {
		if (CollectionUtils.isEmpty(groovyInfos)) {
			return;
		}
		this.registry = (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();
		for (GroovyInfo groovyInfo : groovyInfos) {
			try {
				Class clazz = groovyClassLoader.parseClass(groovyInfo.getGroovyContent());
				BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
				BeanDefinition beanDefinition = builder.getBeanDefinition();
				registry.registerBeanDefinition(groovyInfo.getBeanName(), beanDefinition);
			} catch (BeanDefinitionStoreException e) {
				log.error("当前Groovy脚本执行失败："+JSON.toJSONString(groovyInfo));
				e.printStackTrace();
			} catch (CompilationFailedException e) {
				log.error("当前Groovy脚本执行失败："+JSON.toJSONString(groovyInfo));
				e.printStackTrace();
			}catch (Exception e) {
				log.error("当前Groovy脚本执行失败："+JSON.toJSONString(groovyInfo));
			}
			log.info("当前groovyInfo加载完成,className-{},interfaceId-{},beanName-{},BeanType-{}：",groovyInfo.getClassName(),groovyInfo.getInterfaceId(),groovyInfo.getBeanName(),groovyInfo.getBeanType());
		}

		GroovyInnerCache.put2map(groovyInfos);
	}

	private void init() {

		List<ApiConfig> groovyScripts = apiService.queryApiConfigList();

		apiInfoCache.putAll(groovyScripts);

		List<GroovyInfo> groovyInfos = convert(groovyScripts);

		init(groovyInfos);

		refreshMapping(groovyScripts);
	}

	private void init(List<GroovyInfo> groovyInfos) {

		if (CollectionUtils.isEmpty(groovyInfos)) {
			return;
		}

		ConfigurationXMLWriter config = new ConfigurationXMLWriter();

		addConfiguration(config, groovyInfos);
//		Console.log(JSONUtil.toJsonStr(groovyInfos));

		GroovyInnerCache.put2map(groovyInfos);

		loadBeanDefinitions(config);

	}

	public void refreshNew() {

		List<ApiConfig> groovyScripts = apiService.queryApiConfigList();

		apiInfoCache.putAll(groovyScripts);

		List<GroovyInfo> groovyInfos = convert(groovyScripts);

		if (CollectionUtils.isEmpty(groovyInfos)) {
			return;
		}

		// loadBeanDefinitions 之后才会生效
		destroyBeanDefinition(groovyInfos);

		destroyScriptBeanFactory();

		initNew(groovyInfos);

		GroovyInnerCache.put2map(groovyInfos);

		refreshMapping(groovyScripts);
	}
	

	public void refresh() {

		List<ApiConfig> groovyScripts = apiService.queryApiConfigList();

		apiInfoCache.putAll(groovyScripts);

		List<GroovyInfo> groovyInfos = convert(groovyScripts);

		if (CollectionUtils.isEmpty(groovyInfos)) {
			return;
		}

		// loadBeanDefinitions 之后才会生效
		destroyBeanDefinition(groovyInfos);

		destroyScriptBeanFactory();

		ConfigurationXMLWriter config = new ConfigurationXMLWriter();

		addConfiguration(config, groovyInfos);

		GroovyInnerCache.put2map(groovyInfos);

		loadBeanDefinitions(config);

		refreshMapping(groovyScripts);
	}

	private List<GroovyInfo> convert(List<ApiConfig> list) {

		List<GroovyInfo> groovyInfos = new LinkedList<>();

		if (CollectionUtils.isEmpty(list)) {
			return groovyInfos;
		}

		for (ApiConfig item : list) {
			if (!"Class".equals(item.getScriptType())) {
				continue;
			}
			GroovyInfo groovyInfo = new GroovyInfo();
			groovyInfo.setClassName(item.getBeanName());
			groovyInfo.setGroovyContent(item.getScriptContent());
			groovyInfo.setInterfaceId(item.getInterfaceId());
			groovyInfo.setBeanName(item.getBeanName());
			groovyInfo.setBeanType(item.getScriptType());
			groovyInfos.add(groovyInfo);

		}

		return groovyInfos;
	}

	private void addConfiguration(ConfigurationXMLWriter config, List<GroovyInfo> groovyInfos) {
		for (GroovyInfo groovyInfo : groovyInfos) {
			try {
				groovyClassLoader.parseClass(groovyInfo.getGroovyContent());
			} catch (Exception e) {
				e.printStackTrace();
				log.error("解析Groovy源码失败！");
			}
			DynamicBean bean = new DynamicBean();
			String scriptName = groovyInfo.getClassName();

			Assert.notNull(scriptName, "parser className cannot be empty!");

			// 设置bean的属性，这里只有id和script-source-className。
			bean.put("id", scriptName);
			bean.put("script-source", GroovyConstant.SCRIPT_SOURCE_PREFIX + scriptName);

			config.write(GroovyConstant.SPRING_TAG, bean);
		}
	}

	private void loadBeanDefinitions(ConfigurationXMLWriter config) {

		String contextString = config.getContent();

		if (StringUtils.isBlank(contextString)) {
			return;
		}

		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(
				(BeanDefinitionRegistry) this.applicationContext.getBeanFactory());
		beanDefinitionReader.setResourceLoader(this.applicationContext);
		beanDefinitionReader.setBeanClassLoader(applicationContext.getClassLoader());
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this.applicationContext));

		beanDefinitionReader.loadBeanDefinitions(new InMemoryResource(contextString));

		String[] postProcessorNames = applicationContext.getBeanFactory()
				.getBeanNamesForType(CustomScriptFactoryPostProcessor.class, true, false);

		for (String postProcessorName : postProcessorNames) {
			applicationContext.getBeanFactory()
					.addBeanPostProcessor((BeanPostProcessor) applicationContext.getBean(postProcessorName));
		}
	}

	private void destroyBeanDefinition(List<GroovyInfo> groovyInfos) {
		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext
				.getAutowireCapableBeanFactory();
		for (GroovyInfo groovyInfo : groovyInfos) {
			try {
				beanFactory.removeBeanDefinition(groovyInfo.getClassName());
			} catch (Exception e) {
				System.out
						.println("【Groovy】 delete groovy bean definition exception. skip:" + groovyInfo.getClassName());
			}
		}
	}

	private void destroyScriptBeanFactory() {
		String[] postProcessorNames = applicationContext.getBeanFactory()
				.getBeanNamesForType(CustomScriptFactoryPostProcessor.class, true, false);
		for (String postProcessorName : postProcessorNames) {
			CustomScriptFactoryPostProcessor processor = (CustomScriptFactoryPostProcessor) applicationContext
					.getBean(postProcessorName);
			processor.destroy();
		}
	}

	/**
	 * 重建单一请求的注册与缓存
	 *
	 * @param refreshMapping
	 */
	public void refreshMapping(List<ApiConfig> groovyScripts) {
		try {
			for (ApiConfig apiInfo : groovyScripts) {
				// 取消历史注册
				if (apiInfo != null) {
					requestMappingService.unregisterMappingForApiConfig(apiInfo);
					apiInfoCache.remove(apiInfo);
				}

				// 重新注册mapping
				if (apiInfo != null) {
					requestMappingService.registerMappingForApiConfig(apiInfo);
					apiInfoCache.put(apiInfo);
				}
			}
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}
}
