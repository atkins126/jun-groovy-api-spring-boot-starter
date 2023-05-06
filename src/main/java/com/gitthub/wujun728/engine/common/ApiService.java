package com.gitthub.wujun728.engine.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gitthub.wujun728.engine.util.BeanCopyUtil;
import com.gitthub.wujun728.engine.util.FieldUtils;
import com.gitthub.wujun728.engine.util.JdbcUtil;
import com.google.common.collect.Lists;

//import cn.hutool.core.bean.BeanUtil;
//import cn.hutool.core.bean.copier.CopyOptions;
//import cn.hutool.db.Db;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ApiService {

	@Autowired
	JdbcTemplate jdbcTemplate;

	public String queryCountSql() {
		Long aLong = jdbcTemplate.queryForObject("select count(*) from test ", Long.class);
		return aLong.toString();
	}
	
	@SuppressWarnings("unchecked")
	public List<ApiConfig> queryApiConfigList() {
		List<Map<String, Object>> lists = jdbcTemplate.queryForList("select * from api_config where status = 'ENABLE' ");
		List<ApiConfig> datas = convert(lists,ApiConfig.class);
		if(!CollectionUtils.isEmpty(datas)) {
			datas.stream().map(item->{
				List<ApiSql> sqlList = Lists.newArrayList();
				if("sql".equalsIgnoreCase(item.getScriptType())) {
					String sqls[] = item.getScriptContent().split(";");
					if(sqls.length>0) {
						for(String sql : sqls) {
							if(StringUtils.isEmpty(sql)) {
								continue;
							}
							ApiSql apisql = new ApiSql();
							apisql.setApiId(item.getId());
							apisql.setSqlText(sql);
							sqlList.add(apisql);
						}
					}
				}
				item.setSqlList(sqlList);
					
				return item;
			}
			).collect(Collectors.toList());
		}
		//log.info(JSON.toJSONString(datas));
		return datas;
	}
	
	@SuppressWarnings("unchecked")
	public List<ApiDataSource> queryDatasourceList() {
		List<ApiDataSource> lists = jdbcTemplate.query("select * from api_datasource ",new BeanPropertyRowMapper(ApiDataSource.class));
		return lists;
	}
	
	@SuppressWarnings("unchecked")
	public List<ApiSql> querySQLList(String apiId) {
		List<Map<String, Object>> lists = jdbcTemplate.queryForList("select * from api_sql where api_id = "+apiId);
		List<ApiSql> datas = convert(lists,ApiSql.class);
		//log.info(JSON.toJSONString(datas));
		return datas;
	}
	
	
	@SuppressWarnings("unchecked")
	public List convert(List<Map<String, Object>> lists,Class clazz){
		List datas = Lists.newArrayList();
		if(!CollectionUtils.isEmpty(lists)) {
			lists.forEach(item->{
				Map m = new HashMap<>();
				item.forEach((k,v)->{
					m.put(FieldUtils.columnNameToFieldName(String.valueOf(k)), v);
				});
				Object info = null;
				try {
					info = clazz.newInstance();
					info = JSONObject.parseObject(JSONObject.toJSONString(m), clazz);
//					BeanUtils.copyProperties(m, info);
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				datas.add(info);
			});
			
		}
		//log.info(JSON.toJSONString(datas));
		return datas;
	}
	
	@SuppressWarnings("unchecked")
	public ApiDataSource getDatasource(String id) {
		ApiDataSource info = null;
		List<ApiDataSource> lists = jdbcTemplate.query("select * from api_datasource ",new BeanPropertyRowMapper(ApiDataSource.class));
		if(!CollectionUtils.isEmpty(lists) && lists.size()==1) {
			info = lists.get(0);
		}
		//log.info(JSON.toJSONString(info));
		return info;
	}
	
//    @Cacheable(value = "api", key = "#path", unless = "#result == null")
//    public ApiConfig getConfig(String path) {
//        log.info("get [{}] api config from db",path);
//        ApiConfig apiConfig = apiConfigMapper.selectByPathOnline(path);
//        if(Objects.isNull(apiConfig)){
//            log.warn("can't get [{}] api config from db",path);
//            return null;
//        }
//        List<ApiSqlEntity> apiSqls = apiSqlMapper.selectByApiId(apiConfig.getId());
//        apiConfig.setSqlList(apiSqls);
//        return apiConfig;
//    }

//	public Map<String, Object> getSql() {
//		Map<String, Object> map = new HashMap<>();
//		return map;
//	}
	

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
