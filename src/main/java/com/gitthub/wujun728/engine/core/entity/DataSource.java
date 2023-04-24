package com.gitthub.wujun728.engine.core.entity;

import lombok.Data;

@Data
public class DataSource {

    String id;
    String name;
    String url;
    String username;
    String password;
    String type;
    String driver;

}
