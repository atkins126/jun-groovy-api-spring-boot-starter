package com.gitthub.wujun728.engine.core.entity;

import lombok.Data;

import java.util.List;

@Data
public class ApiConfig {

    String id;

    String name;

    String note;

    String path;

    String datasourceId;

    List<ApiSql> sqlList;
 
    String params;

    Integer status;

    Integer previlege;

    String groupId;

    String cachePlugin;

    String cachePluginParams;

    String createTime;

    String updateTime;

    String contentType;

    Integer openTrans;

    String jsonParam;

    String alarmPlugin;

    String alarmPluginParam;
}
