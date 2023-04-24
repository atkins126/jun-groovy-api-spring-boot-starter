package com.gitthub.wujun728.engine.core.entity;

import lombok.Data;

@Data
public class ApiSql {

    Integer id;

    String apiId;

    String sqlText;

    String transformPlugin;

    String transformPluginParams;

}
