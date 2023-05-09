package com.gitthub.wujun728.engine.groovy.mapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.gitthub.wujun728.engine.common.ApiConfig;

//import cn.hutool.core.lang.Console;
//import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RequestMappingService implements InitializingBean {

	@Autowired
	private RequestMappingHandlerMapping requestMappingHandlerMapping;

	@Autowired
	private ApiProperties apiProperties;

	@Autowired
	@Lazy
	private RequestMappingExecutor mappingFactory;

	/**
	 * 获取已注册的API地址
	 */
	public List<ApiConfig> getPathListForCode() {

		Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
		List<ApiConfig> result = new ArrayList<>(map.size());
		for (RequestMappingInfo info : map.keySet()) {

			if (map.get(info).getMethod().getDeclaringClass() == RequestMappingExecutor.class) {
				continue;
			}

			String groupName = map.get(info).getBeanType().getSimpleName();
			for (String path : getPatterns(info)) {

				// 过滤本身的类
				if (path.indexOf(apiProperties.getBaseRegisterPath()) == 0 || path.equals("/error")) {
					continue;
				}

				Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
				if (methods.isEmpty()) {
					ApiConfig apiInfo = new ApiConfig();
					apiInfo.setPath(path);
					apiInfo.setMethod("All");
					apiInfo.setScriptType("Code");
					apiInfo.setBeanName(apiProperties.getServiceName());
					apiInfo.setCreator("admin");
					apiInfo.setDatasourceId("");
					apiInfo.setScriptContent("");
					apiInfo.setPath(path);
					result.add(apiInfo);
				} else {
					for (RequestMethod method : methods) {
						ApiConfig apiInfo = new ApiConfig();
						apiInfo.setPath(path);
						apiInfo.setMethod(method.name());
						apiInfo.setScriptType("Code");
						apiInfo.setBeanName(apiProperties.getServiceName());
						apiInfo.setCreator("admin");
						apiInfo.setDatasourceId("");
						apiInfo.setScriptContent("");
						apiInfo.setPath(path);
						result.add(apiInfo);
					}
				}

			}
		}
		return result;
	}
	


	/**
	 * 注册mapping
	 *
	 * @param apiInfo
	 */
	public synchronized void registerMappingForApiConfig(ApiConfig apiInfo) throws NoSuchMethodException {
		if ("Code".equals(apiInfo.getScriptType())) {
			return;
		}

		String pattern = apiInfo.getPath();

		if (StringUtils.isEmpty(pattern) || pattern.startsWith("TEMP-")) {
			return;
		}

		RequestMappingInfo mappingInfo = getRequestMappingInfo(pattern, apiInfo.getMethod());
		if (mappingInfo != null) {
			return;
		}

		log.debug("Mapped [{}]{}", apiInfo.getMethod(), pattern);
		if(!StringUtils.isEmpty(apiInfo.getMethod())) {
			mappingInfo = RequestMappingInfo.paths(pattern).methods(RequestMethod.valueOf(apiInfo.getMethod())).build();
		}else {
			mappingInfo = RequestMappingInfo.paths(pattern).build();
		}
		Method targetMethod = RequestMappingExecutor.class.getDeclaredMethod("execute",HttpServletRequest.class, HttpServletResponse.class);
		requestMappingHandlerMapping.registerMapping(mappingInfo, mappingFactory, targetMethod);
	}

	/**
	 * 取消注册mapping
	 *
	 * @param apiInfo
	 */
	public synchronized void unregisterMappingForApiConfig(ApiConfig apiInfo) {
		if ("Code".equals(apiInfo.getScriptType())) {
			return;
		}

		String pattern = apiInfo.getPath();

		if (StringUtils.isEmpty(pattern) || pattern.startsWith("TEMP-")) {
			return;
		}

		RequestMappingInfo mappingInfo = getRequestMappingInfo(pattern, apiInfo.getMethod());
		if (mappingInfo == null) {
			return;
		}

		log.info("Cancel Mapping [{}]{}", apiInfo.getMethod()==null?"":apiInfo.getMethod(), pattern);
		if(!StringUtils.isEmpty(apiInfo.getMethod())) {
			mappingInfo = RequestMappingInfo.paths(pattern).methods(RequestMethod.valueOf(apiInfo.getMethod())).build();
		}else {
			mappingInfo = RequestMappingInfo.paths(pattern).build();
		}
		requestMappingHandlerMapping.unregisterMapping(mappingInfo);
	}

	private RequestMappingInfo getRequestMappingInfo(String pattern, String method) {
		Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
		for (RequestMappingInfo info : map.keySet()) {
			Set<String> patterns = getPatterns(info);
			Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
			if (patterns.contains(pattern) && (methods.isEmpty() || methods.contains(RequestMethod.valueOf(method)))) {
				return info;
			}
		}
		return null;
	}

	/**
	 * 判断是否是原始代码注册的mapping
	 * 
	 * @param method
	 * @param pattern
	 */
	public Boolean isCodeMapping(String pattern, String method) {
		Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
		for (RequestMappingInfo info : map.keySet()) {
			if (map.get(info).getMethod().getDeclaringClass() == RequestMappingExecutor.class) {
				continue;
			}
			Set<String> patterns = getPatterns(info);
			Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
			if (patterns.contains(pattern) && (methods.isEmpty() || methods.contains(RequestMethod.valueOf(method)))) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		log.info(" RequestMappingService is init .... ");
	}

	public static Set<String> getPatterns(RequestMappingInfo info) {
		return info.getPatternsCondition() == null ? /* info.getPathPatternsCondition().getPatternValues() */null
				: info.getPatternsCondition().getPatterns();
	}

}
