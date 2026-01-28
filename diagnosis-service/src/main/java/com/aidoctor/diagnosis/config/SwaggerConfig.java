package com.aidoctor.diagnosis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;
import springfox.documentation.spring.web.readers.operation.HandlerMethodResolver;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Swagger配置
 * 修复 Springfox 与 Spring Boot 2.7+ 的兼容性问题
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.aidoctor.diagnosis.controller"))
            .paths(PathSelectors.any())
            .build()
            .apiInfo(apiInfo());
    }
    
    /**
     * 修复 Springfox 与 Spring Boot 2.7+ 的兼容性问题
     * 解决 NullPointerException: WebMvcPatternsRequestConditionWrapper.getPatterns
     * 
     * Spring Boot 2.7+ 改变了路径匹配策略，Springfox 3.0.0 需要过滤掉使用 PathPatternParser 的 HandlerMapping
     */
    @Bean
    public WebMvcRequestHandlerProvider webMvcRequestHandlerProvider(
            Optional<ServletContext> servletContext,
            HandlerMethodResolver methodResolver,
            Optional<List<RequestMappingInfoHandlerMapping>> handlerMappings) {
        List<RequestMappingInfoHandlerMapping> filteredMappings = handlerMappings.orElse(Collections.emptyList())
                .stream()
                .filter(mapping -> mapping.getPatternParser() == null)
                .collect(Collectors.toList());
        
        return new WebMvcRequestHandlerProvider(
                servletContext,
                methodResolver,
                filteredMappings
        );
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("智能诊断系统API文档")
            .description("智能诊断系统的REST API接口文档")
            .version("1.0.0")
            .build();
    }
}

