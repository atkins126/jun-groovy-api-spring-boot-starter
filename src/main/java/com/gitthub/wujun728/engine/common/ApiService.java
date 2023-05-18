package com.gitthub.wujun728.engine.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.gitthub.wujun728.engine.util.FieldUtils;
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
	


}
