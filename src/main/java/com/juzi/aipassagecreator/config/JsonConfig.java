package com.juzi.aipassagecreator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Spring MVC Json 配置
 *
 * 让 Jackson 在把 Java 对象转换成 JSON 时，将 Long 和 long 类型的值输出为字符串。
 * 这样，前端用 JavaScript 读取大整数时，就不会因为 Number 的精度限制而改变数值。
 */
@JsonComponent
public class JsonConfig {

    /**
     * 添加 Long 转 json 精度丢失的配置
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(
            Jackson2ObjectMapperBuilder builder) {

        ObjectMapper objectMapper = builder
                .createXmlMapper(false)
                .build();

//        SimpleModule 是一个存放 Jackson 扩展规则的容器，可以在里面注册自定义序列化器、反序列化器等。
        SimpleModule module = new SimpleModule();

        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);

        objectMapper.registerModule(module);

        return objectMapper;
    }
}