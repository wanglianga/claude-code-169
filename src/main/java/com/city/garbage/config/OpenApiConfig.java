package com.city.garbage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("城市智能垃圾房满溢异味投放违规联动服务 API")
                .version("1.0.0")
                .description("设备上报 -> 告警合并为治理联动单 -> 物业保洁消杀/环卫清运/积分申诉/投诉整改联动闭环；"
                        + "设备端、居民端、物业端、环卫端、街道端共享同一垃圾房状态。"));
    }
}
