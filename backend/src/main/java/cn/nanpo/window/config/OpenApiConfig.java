package cn.nanpo.window.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI nanpoWindowOpenApi() {
        return new OpenAPI().info(new Info()
                .title("南坡之窗 API")
                .version("0.1.0")
                .description("公开门户、农户经营台与村庄运营后台的统一 API。"));
    }
}

