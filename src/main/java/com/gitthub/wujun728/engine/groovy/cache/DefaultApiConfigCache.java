package com.gitthub.wujun728.engine.groovy.cache;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.gitthub.wujun728.engine.common.ApiConfig;
import com.gitthub.wujun728.engine.util.IdUtil;

//import cn.hutool.core.util.IdUtil;
//import cn.hutool.core.util.StrUtil;

/**
 * API信息缓存
 */
@Component
public class DefaultApiConfigCache implements IApiConfigCache {

    private Map<String, ApiConfig> cacheApiConfig = new ConcurrentHashMap<>();

    private String instanceId = IdUtil.generateUUID();

    @Override
    public ApiConfig get(ApiConfig apiInfo){
        return cacheApiConfig.get(buildApiConfigKey(apiInfo));
    }
    @Override
    public ApiConfig get(String path){
    	return cacheApiConfig.get(path);
    }

    @Override
    public Collection<ApiConfig> getAll() {
        return cacheApiConfig.values();
    }

    @Override
    public void removeAll() {
        cacheApiConfig.clear();
    }

    @Override
    public void remove(ApiConfig apiInfo) {
        cacheApiConfig.remove(buildApiConfigKey(apiInfo));
    }

    @Override
    public void put(ApiConfig apiInfo) {
        cacheApiConfig.put(buildApiConfigKey(apiInfo),apiInfo);
    }
    @Override
    public void putAll(List<ApiConfig> apiInfos) {
    	this.removeAll();
    	for(ApiConfig apiInfo : apiInfos) {
    		this.put(apiInfo);
    	}
    }

    private String buildApiConfigKey(ApiConfig apiInfo) {
//    	if(StrUtil.isNotEmpty(apiInfo.getMethod())) {
//    		return apiInfo.getMethod() +" "+ apiInfo.getPath();
//    	}
    	return apiInfo.getPath();
    }

}
