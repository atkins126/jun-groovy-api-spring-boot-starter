package com.gitthub.wujun728.engine.groovy.mapping;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitthub.wujun728.engine.common.ApiConfig;
import com.gitthub.wujun728.engine.common.ApiDataSource;
import com.gitthub.wujun728.engine.common.ApiProperties;
import com.gitthub.wujun728.engine.common.ApiService;
import com.gitthub.wujun728.engine.common.ApiSql;
import com.gitthub.wujun728.engine.common.DataResult;
import com.gitthub.wujun728.engine.groovy.cache.IApiConfigCache;
import com.gitthub.wujun728.engine.plugin.CachePlugin;
import com.gitthub.wujun728.engine.plugin.PluginManager;
import com.gitthub.wujun728.engine.plugin.TransformPlugin;
import com.gitthub.wujun728.engine.util.BeanCopyUtil;
import com.gitthub.wujun728.engine.util.JdbcUtil;
import com.gitthub.wujun728.engine.util.PoolManager;
import com.gitthub.wujun728.mybatis.sql.SqlMeta;
import com.google.common.collect.Lists;

//import cn.hutool.core.bean.BeanUtil;
//import cn.hutool.core.lang.Console;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RequestMappingExecutor implements ApplicationListener<ContextRefreshedEvent> {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ApiProperties apiProperties;

	@Autowired
	private ApiService apiService;

	@Autowired
	private IApiConfigCache apiInfoCache;

	@Autowired
	private ServerProperties serverProperties;

	private List<String> bodyMethods = Arrays.asList("POST", "PUT", "PATCH");

	public void init(Boolean isStart) throws Exception {

	}

	private String getIpAndAdress() {
		String content = serverProperties.getServlet().getContextPath() == null ? ""
				: serverProperties.getServlet().getContextPath();
		Integer port = serverProperties.getPort() == null ? 8080 : serverProperties.getPort();
		return "http://localhost:" + port + ("/" + content + apiProperties.getContext()).replace("//", "/");
	}

	/**
	 * 执行脚本逻辑
	 */
	@RequestMapping
	@ResponseBody
	public void execute(HttpServletRequest request, HttpServletResponse response) throws Throwable {
		Class<? extends RequestMappingExecutor> cls = this.getClass();
		// 使用方法
		try {
			// 获取方法 Method 对象
			Method method = cls.getDeclaredMethod("process", HttpServletRequest.class, HttpServletResponse.class);
			// 执行方法
			method.invoke(this, request, response);
		} catch (NoSuchMethodException e) {
			defaultMetod(request, response);
			// 找不到当前子类实现的方法[process]，走默认方法的逻辑
			log.warn("找不到当前子类实现的方法[process]，走默认方法的逻辑");
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 执行脚本逻辑
	 */
	public void defaultMetod(HttpServletRequest request, HttpServletResponse response) throws Throwable {
		log.info("servlet execute");
		String servletPath = request.getRequestURI();
		PrintWriter out = null;
		try {
			out = response.getWriter();
			DataResult DataResult = process(servletPath, request, response);
			out.append(JSON.toJSONString(DataResult));

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out.append(JSON.toJSONString(DataResult.fail(e.toString())));
			log.error(e.toString(), e);
		} finally {
			if (out != null)
				out.close();
		}
	}

	public DataResult process(String path, HttpServletRequest request, HttpServletResponse response) {
		System.out.println("servlet execute");
		// 校验接口是否存在
		ApiConfig config = apiInfoCache.get(path);
		if (config == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			log.info("servlet execute");
			return DataResult.fail("Api not exists");
		}
		try {
			ApiDataSource datasource = apiService.getDatasource(config.getDatasourceId());
			if (datasource == null) {
				response.setStatus(500);
				return DataResult.fail("Datasource not exists!");
			}
			Map<String, Object> sqlParam = getParams(request, config);
			List<ApiSql> sqlList = config.getSqlList();
			if (CollectionUtils.isEmpty(sqlParam) && !CollectionUtils.isEmpty(sqlList)
					&& JSON.toJSONString(sqlList).contains("#")) {
				return DataResult.fail("Request parameter is not exists(请求入参不能为空)!");
			}
			ApiDataSource ds = new ApiDataSource();
			BeanCopyUtil.copyField(datasource, ds);
			DruidPooledConnection connection = PoolManager.getPooledConnection(ds);
			// 是否开启事务
			boolean flag = config.getOpenTrans() == 1 ? true : false;
			// 执行sql
			List<Object> dataList = executeSql(connection, sqlList, sqlParam, flag);
			// 执行数据转换
			for (int i = 0; i < sqlList.size(); i++) {
				ApiSql apiSql = sqlList.get(i);
				Object data = dataList.get(i);
				// 如果此单条sql是查询类sql，并且配置了数据转换插件
				if (data instanceof Iterable && StringUtils.isNotBlank(apiSql.getTransformPlugin())) {
					log.info("transform plugin execute");
					List<JSONObject> sourceData = (List<JSONObject>) (data); // 查询类sql的返回结果才可以这样强制转换，只有查询类sql才可以配置转换插件
					TransformPlugin transformPlugin = (TransformPlugin) PluginManager
							.getPlugin(apiSql.getTransformPlugin());
					Object resData = transformPlugin.transform(sourceData, apiSql.getTransformPluginParams());
					dataList.set(i, resData);// 重新设置值
				}
			}
			Object res = dataList;
			// 如果只有单条sql,返回结果不是数组格式
			if (dataList.size() == 1) {
				res = dataList.get(0);
			}
			// 设置缓存
			if (StringUtils.isNoneBlank(config.getCachePlugin())) {
				CachePlugin cachePlugin = (CachePlugin) PluginManager.getPlugin(config.getCachePlugin());
				ApiConfig apiConfig = new ApiConfig();
				BeanCopyUtil.copyField(datasource, apiConfig);
				cachePlugin.set(apiConfig, sqlParam, res);
			}
			return DataResult.success(res);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	public List<Object> executeSql(Connection connection, List<ApiSql> sqlList, Map<String, Object> sqlParam,
			boolean flag) {
		List<Object> dataList = new ArrayList<>();
		try {
			if (flag)
				connection.setAutoCommit(false);
			else
				connection.setAutoCommit(true);
			for (ApiSql apiSql : sqlList) {
				SqlMeta sqlMeta = JdbcUtil.getEngine().parse(apiSql.getSqlText(), sqlParam);
				Object data = JdbcUtil.executeSql(connection, sqlMeta.getSql(), sqlMeta.getJdbcParamValues());
				dataList.add(data);
			}
			if (flag)
				connection.commit();
			return dataList;
		} catch (Exception e) {
			try {
				if (flag)
					connection.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new RuntimeException(e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private List<String> getSQLs(ApiConfig apiConfig) {
		if (StringUtils.isNotEmpty(apiConfig.getScriptContent())) {
			return Arrays.asList(apiConfig.getScriptContent().split("###"));
		}
		return Lists.newArrayList();

	}

	protected Map<String, Object> getParams(HttpServletRequest request, ApiConfig apiConfig) {
		String unParseContentType = request.getContentType();

		// 如果是浏览器get请求过来，取出来的contentType是null
		if (unParseContentType == null) {
			unParseContentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE;
		}
		// issues/I57ZG2
		// 解析contentType 格式: appliation/json;charset=utf-8
		String[] contentTypeArr = unParseContentType.split(";");
		String contentType = contentTypeArr[0];
		Map<String, Object> params = null;
		// 如果是application/json请求，不管接口规定的content-type是什么，接口都可以访问，且请求参数都以json body 为准
		if (contentType.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
			JSONObject jo = getHttpJsonBody(request);
			if (!ObjectUtils.isEmpty(jo)) {
				params = JSONObject.parseObject(jo.toJSONString(), new TypeReference<Map<String, Object>>() {
				});
			}
		}
		// 如果是application/x-www-form-urlencoded请求，先判断接口规定的content-type是不是确实是application/x-www-form-urlencoded
		else if (contentType.equalsIgnoreCase(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
			if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equalsIgnoreCase(contentType)) {
				params = getSqlParam(request, apiConfig);
			} else if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equalsIgnoreCase(apiConfig.getContentType())) {
				params = getSqlParam(request, apiConfig);
			} else {
				params = getSqlParam(request, apiConfig);
				System.err.println("this API only support content-type: " + apiConfig.getContentType()
						+ ", but you use: " + contentType);
			}
		} else {
			params = getSqlParam(request, apiConfig);
			throw new RuntimeException("content-type not supported: " + contentType);
		}
		String uri = request.getRequestURI();
		Map<String, String> header = RequestMappingExecutor.buildHeaderParams(request);
		Map<String, Object> session = RequestMappingExecutor.buildSessionParams(request);
		Map<String, Object> urivar = this.getParam(request);
		String pattern = RequestMappingExecutor.buildPattern(request);
		Map<String, String> pathvar = this.getPathVar(pattern, uri);
		Map<String, Object> params1 = RequestMappingExecutor.getParameters(request);
		if (!CollectionUtils.isEmpty(session)) {
			params.putAll(session);
		}
		if (!CollectionUtils.isEmpty(header)) {
			params.putAll(header);
		}
		if (!CollectionUtils.isEmpty(pathvar)) {
			params.putAll(pathvar);
		}
		if (!CollectionUtils.isEmpty(urivar)) {
			params.putAll(urivar);
		}
		if (!CollectionUtils.isEmpty(params1)) {
			params.putAll(params1);
		}
		params.put("path",apiConfig.getPath());
		return params;
	}

	protected JSONObject getHttpJsonBody(HttpServletRequest request) {
		try {
			InputStreamReader in = new InputStreamReader(request.getInputStream(), "utf-8");
			BufferedReader br = new BufferedReader(in);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			br.close();
			JSONObject jsonObject = JSON.parseObject(sb.toString());
			return jsonObject;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {

		}
		return null;
	}

	@SneakyThrows
	@Override
	public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
		ApplicationContext parent = contextRefreshedEvent.getApplicationContext().getParent();
		if (parent == null) {
			init(true);
		}
	}

	public static String buildPattern(HttpServletRequest request) {
		return (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
	}

	public static Map<String, Object> buildSessionParams(HttpServletRequest request) {
		Enumeration<String> keys = request.getSession().getAttributeNames();
		Map<String, Object> result = new HashMap<>();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			result.put(key, request.getSession().getAttribute(key));
		}
		return result;
	}

	public static Map<String, String> buildHeaderParams(HttpServletRequest request)
	/* throws UnsupportedEncodingException */ {
		Enumeration<String> headerKeys = request.getHeaderNames();
		Map<String, String> result = new HashMap<>();
		while (headerKeys.hasMoreElements()) {
			String key = headerKeys.nextElement();
			String value = request.getHeader(key);
			result.put(key, value);
		}
		return result;
	}

	private Map<String, String> getPathVar(String pattern, String url) {
		Integer beginIndex = url.indexOf("/", 8);
		if (beginIndex == -1) {
			return null;
		}
		Integer endIndex = url.indexOf("?") == -1 ? url.length() : url.indexOf("?");
		String path = url.substring(beginIndex, endIndex);
		AntPathMatcher matcher = new AntPathMatcher();
		if (matcher.match(pattern, path)) {
			return matcher.extractUriTemplateVariables(pattern, path);
		}
		return null;
	}

	private Map<String, Object> getParam(HttpServletRequest request) {
		StringBuffer url = request.getRequestURL();
        if (request.getQueryString() != null) {
            url.append("?");
            url.append(request.getQueryString());
        }
		Map<String, Object> result = new HashMap<>();
		MultiValueMap<String, String> urlMvp = UriComponentsBuilder.fromHttpUrl(url.toString()).build().getQueryParams();
		urlMvp.forEach((key, value) -> {
			String firstValue = CollectionUtils.isEmpty(value) ? null : value.get(0);
			result.put(key, firstValue);
		});
		return result;
	}

	public static Map<String, Object> getParameters(HttpServletRequest request) {
		Map<String, Object> map = new HashMap<String, Object>();
		Enumeration paramNames = request.getParameterNames();
		while (paramNames.hasMoreElements()) {
			String paramName = (String) paramNames.nextElement();
			String[] paramValues = request.getParameterValues(paramName);
			if (paramValues.length > 0) {
				String paramValue = paramValues[0];
				if (paramValue.length() != 0) {
					map.put(paramName, paramValue);
				}
			}
		}
		Set<Map.Entry<String, Object>> set = map.entrySet();
		log.debug("==============================================================");
		for (Map.Entry entry : set) {
			log.debug(entry.getKey() + ":" + entry.getValue());
		}
		log.debug("=============================================================");
		return map;
	}
	
	

    public Map<String, Object> getSqlParam(HttpServletRequest request, ApiConfig config) {
        Map<String, Object> map = new HashMap<>();

        JSONArray requestParams = JSON.parseArray(config.getParams());
        for (int i = 0; i < requestParams.size(); i++) {
            JSONObject jo = requestParams.getJSONObject(i);
            String name = jo.getString("name");
            String type = jo.getString("type");

            //数组类型参数
            if (type.startsWith("Array")) {
                String[] values = request.getParameterValues(name);
                if (values != null) {
                    List<String> list = Arrays.asList(values);
                    if (values.length > 0) {
                        switch (type) {
                            case "Array<double>":
                                List<Double> collect = list.stream().map(value -> Double.valueOf(value)).collect(Collectors.toList());
                                map.put(name, collect);
                                break;
                            case "Array<bigint>":
                                List<Long> longs = list.stream().map(value -> Long.valueOf(value)).collect(Collectors.toList());
                                map.put(name, longs);
                                break;
                            case "Array<string>":
                            case "Array<date>":
                                map.put(name, list);
                                break;
                        }
                    } else {
                        map.put(name, list);
                    }
                } else {
                    map.put(name, null);
                }
            } else {

                String value = request.getParameter(name);
                if (StringUtils.isNotBlank(value)) {

                    switch (type) {
                        case "double":
                            Double v = Double.valueOf(value);
                            map.put(name, v);
                            break;
                        case "bigint":
                            Long longV = Long.valueOf(value);
                            map.put(name, longV);
                            break;
                        case "string":
                        case "date":
                            map.put(name, value);
                            break;
                    }
                } else {
                    map.put(name, value);
                }
            }
        }
        return map;
    }

}
