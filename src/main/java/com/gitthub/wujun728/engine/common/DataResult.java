package com.gitthub.wujun728.engine.common;

import lombok.Data;

/**
 * 返回值DataResult
 *
 * @author wujun
 * @version V1.0
 * @date 2020年3月18日
 */
@Data
public class DataResult {
	

    /**
     * 请求响应code，0为成功 其他为失败
     */
    private int code;

    /**
     * 响应异常码详细信息
     */
    private String msg;

    private Object data;
    
    boolean success;
 

    public DataResult(int code, Object data) {
        this.code = code;
        this.data = data;
        this.msg = null;
    }

    public DataResult(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public DataResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
        this.data = null;
    }


    public DataResult() {
        this.code = 0;
        this.msg = "操作成功";
        this.data = null;
    }

    public DataResult(Object data) {
        this.data = data;
        this.code = 0;
        this.msg = "操作成功";
    }
    public DataResult(Object data,int code,String msg) {
        this.data = data;
        this.code = code;
        this.msg = msg;
    } 

    /**
     * 自定义返回  data为null
     */
    public static DataResult getResult(int code, String msg) {
        return new DataResult(code, msg);
    }

    

    /**
     * 操作成功 data为null
     */
    public static DataResult success() {
        return new DataResult();
    }

    /**
     * 操作成功 data 不为null
     */
    public static DataResult success(Object data) {
        return new DataResult(data);
    }

    /**
     * 操作失败 data 不为null
     */
    public static DataResult fail(String msg) {
        return new DataResult(500002, msg);
    }
    
    public static DataResult successWithMsg(String msg) {
    	return new DataResult(null,0,msg);
    }

    public static DataResult successWithData(Object data) {
    	return new DataResult(data,0,"操作成功");
    }
    


}
