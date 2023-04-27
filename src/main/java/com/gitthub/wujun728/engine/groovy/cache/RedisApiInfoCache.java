package com.gitthub.wujun728.engine.groovy.cache;
//package com.jun.plugin.engine.core.mapping.base.cache;
//
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.ConvertingCursor;
//import org.springframework.data.redis.core.Cursor;
//import org.springframework.data.redis.core.ScanOptions;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.data.redis.serializer.RedisSerializer;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.jun.plugin.engine.core.groovy.core.script.entity.ApiConfig;
//import com.jun.plugin.engine.core.mapping.base.ApiProperties;
//
///**
// * 集群通知
// */
//public class RedisApiConfigCache implements IApiConfigCache{
//
//    @Autowired
//    private StringRedisTemplate redisTemplate;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private ApiProperties rocketApiProperties;
//
//    private String buildPrefix(){
//        return "rocket-api:"+rocketApiProperties.getServiceName();
//    }
//
//    private String buildApiConfigKey(ApiConfig apiInfo) {
//        return buildPrefix()+":"+apiInfo.getMethod() +"-"+ apiInfo.getPath();
//    }
//
//    @Override
//    public ApiConfig get(ApiConfig apiInfo) {
//        String strValue = redisTemplate.opsForValue().get(buildApiConfigKey(apiInfo));
//        try {
//            return objectMapper.readValue(strValue,ApiConfig.class);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    @Override
//    public void put(ApiConfig apiInfo) {
//        try {
//            String strValue = objectMapper.writeValueAsString(apiInfo);
//            redisTemplate.opsForValue().set(buildApiConfigKey(apiInfo),strValue);
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void remove(ApiConfig apiInfo) {
//        redisTemplate.delete(buildApiConfigKey(apiInfo));
//    }
//
//    @Override
//    public void removeAll() {
//        redisTemplate.delete(getKeys());
//    }
//
//    private List<String> getKeys(){
//        String patternKey = buildPrefix()+":*";
//        ScanOptions options = ScanOptions.scanOptions()
//                .count(10000)
//                .match(patternKey).build();
//        RedisSerializer<String> redisSerializer = (RedisSerializer<String>) redisTemplate.getKeySerializer();
//        Cursor cursor = (Cursor) redisTemplate.executeWithStickyConnection(redisConnection -> new ConvertingCursor<>(redisConnection.scan(options), redisSerializer::deserialize));
//        List<String> keys = new ArrayList<>();
//        while(cursor.hasNext()){
//            keys.add(cursor.next().toString());
//        }
//        try {
//            cursor.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return keys;
//    }
//
//    @Override
//    public Collection<ApiConfig> getAll() {
//        return redisTemplate.opsForValue().multiGet(getKeys()).stream().map(item->{
//            try {
//                return objectMapper.readValue(item,ApiConfig.class);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return null;
//        }).collect(Collectors.toList());
//    }
//
//	@Override
//	public void putAll(List<ApiConfig> apiInfos) {
//		for(ApiConfig apiInfo : apiInfos) {
//    		this.put(apiInfo);
//    	}
//	}
//}
