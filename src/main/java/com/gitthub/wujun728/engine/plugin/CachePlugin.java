package com.gitthub.wujun728.engine.plugin;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitthub.wujun728.engine.entity.ApiConfig;

public abstract class CachePlugin implements BasePlugin {

    public Logger logger = LoggerFactory.getLogger(CachePlugin.class);

    /**
     * 缓存设置
     *
     * @param config api配置
     * @param requestParams request参数
     * @param data   要缓存的数据
     */
    public abstract void set(ApiConfig config, Map<String, Object> requestParams, Object data);

    /**
     * 清除所有缓存，API修改、删除、下线的时候会触发清除缓存
     *
     * @param config api配置
     */
    public abstract void clean(ApiConfig config);

    /**
     * 查询缓存
     *
     * @param config api配置
     * @param requestParams request参数
     * @return
     */
    public abstract Object get(ApiConfig config, Map<String, Object> requestParams);
}
