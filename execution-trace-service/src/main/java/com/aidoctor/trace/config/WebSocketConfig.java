package com.aidoctor.trace.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // 启用简单消息代理，客户端订阅/topic前缀的路径
        config.setApplicationDestinationPrefixes("/app"); // 客户端发送消息到/app前缀的路径
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/v1/trace/ws") // WebSocket连接端点
            .setAllowedOriginPatterns("*") // 允许所有来源连接，生产环境需要限制
            .withSockJS(); // 启用SockJS支持
    }
}


