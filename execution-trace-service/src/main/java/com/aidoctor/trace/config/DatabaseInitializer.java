package com.aidoctor.trace.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据库初始化器
 * 在应用启动时自动创建数据库（如果不存在）
 * 
 * 配置项：execution.trace.auto-create-database=true（默认启用）
 * 
 * 注意：此初始化器会在数据源初始化之前执行，确保数据库存在
 */
@Slf4j
@ConditionalOnProperty(
    name = "execution.trace.auto-create-database",
    havingValue = "true",
    matchIfMissing = true // 默认启用
)
public class DatabaseInitializer implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    
    private volatile boolean initialized = false;
    
    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        if (!initialized) {
            Environment environment = event.getEnvironment();
            initializeDatabase(environment);
            initialized = true;
        }
    }
    
    private void initializeDatabase(Environment environment) {
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");
        
        if (jdbcUrl == null || username == null) {
            log.warn("数据源配置不完整，跳过自动创建数据库");
            return;
        }
        
        try {
            // 从JDBC URL中提取数据库名称
            String databaseName = extractDatabaseName(jdbcUrl);
            if (databaseName == null || databaseName.isEmpty()) {
                log.warn("无法从JDBC URL中提取数据库名称，跳过自动创建数据库");
                return;
            }
            
            // 构建连接到MySQL服务器的URL（不指定数据库）
            String serverUrl = buildServerUrl(jdbcUrl);
            
            log.info("检查数据库是否存在: {}", databaseName);
            log.debug("连接到MySQL服务器URL: {}", serverUrl);
            
            // 连接到MySQL服务器
            try (Connection connection = DriverManager.getConnection(serverUrl, username, password != null ? password : "");
                 Statement statement = connection.createStatement()) {
                
                // 检查数据库是否存在
                boolean databaseExists = checkDatabaseExists(statement, databaseName);
                
                if (!databaseExists) {
                    log.info("数据库不存在，开始创建: {}", databaseName);
                    createDatabase(statement, databaseName);
                    log.info("数据库创建成功: {}", databaseName);
                } else {
                    log.info("数据库已存在: {}", databaseName);
                }
            }
            
        } catch (Exception e) {
            log.error("自动创建数据库失败，请手动创建数据库: {}", e.getMessage());
            log.debug("自动创建数据库失败详情", e);
            // 不抛出异常，让应用继续启动，用户可能需要手动创建数据库
        }
    }
    
    /**
     * 从JDBC URL中提取数据库名称
     * 格式：jdbc:mysql://host:port/database?params
     */
    private String extractDatabaseName(String jdbcUrl) {
        // jdbc:mysql://localhost:3306/aidoctor_trace?useUnicode=true&serverTimezone=Asia/Shanghai&...
        try {
            // 找到协议后的 "://"
            int protocolEnd = jdbcUrl.indexOf("://");
            if (protocolEnd == -1) {
                log.warn("JDBC URL格式不正确，找不到协议部分: {}", jdbcUrl);
                return null;
            }
            
            // 找到协议后的第一个 "/"（这应该是数据库名的开始）
            // 跳过 "://" 后的主机名和端口号
            int dbStart = jdbcUrl.indexOf('/', protocolEnd + 3);
            if (dbStart == -1) {
                log.warn("JDBC URL格式不正确，找不到数据库名部分: {}", jdbcUrl);
                return null;
            }
            
            // 找到参数开始的位置 "?"
            int questionMark = jdbcUrl.indexOf('?', dbStart);
            if (questionMark == -1) {
                // 没有参数，直接返回数据库名
                return jdbcUrl.substring(dbStart + 1);
            } else {
                // 有参数，返回数据库名部分
                return jdbcUrl.substring(dbStart + 1, questionMark);
            }
        } catch (Exception e) {
            log.error("提取数据库名称失败: {}", jdbcUrl, e);
            return null;
        }
    }
    
    /**
     * 构建连接到MySQL服务器的URL（连接到mysql系统数据库）
     * 格式：jdbc:mysql://host:port/mysql?params
     */
    private String buildServerUrl(String jdbcUrl) {
        // jdbc:mysql://localhost:3306/aidoctor_trace?useUnicode=true&serverTimezone=Asia/Shanghai&...
        // 转换为: jdbc:mysql://localhost:3306/mysql?useUnicode=true&serverTimezone=Asia/Shanghai&...
        try {
            // 找到协议后的 "://"
            int protocolEnd = jdbcUrl.indexOf("://");
            if (protocolEnd == -1) {
                log.warn("JDBC URL格式不正确，找不到协议部分: {}", jdbcUrl);
                return jdbcUrl;
            }
            
            // 找到协议后的第一个 "/"（这应该是数据库名的开始）
            int dbStart = jdbcUrl.indexOf('/', protocolEnd + 3);
            if (dbStart == -1) {
                // 没有数据库名部分，添加mysql系统数据库
                int questionMark = jdbcUrl.indexOf('?');
                if (questionMark == -1) {
                    return jdbcUrl + "/mysql";
                } else {
                    return jdbcUrl.substring(0, questionMark) + "/mysql" + jdbcUrl.substring(questionMark);
                }
            }
            
            // 构建新的URL：保留协议和主机端口部分，替换数据库名为mysql系统数据库，保留参数
            String baseUrl = jdbcUrl.substring(0, dbStart + 1); // jdbc:mysql://localhost:3306/
            String params = "";
            
            // 查找参数部分
            int questionMark = jdbcUrl.indexOf('?', dbStart);
            if (questionMark != -1) {
                params = jdbcUrl.substring(questionMark); // ?useUnicode=true&...
            }
            
            // 使用mysql系统数据库连接（这个数据库总是存在的）
            return baseUrl + "mysql" + params;
        } catch (Exception e) {
            log.error("构建服务器URL失败: {}", jdbcUrl, e);
            return jdbcUrl;
        }
    }
    
    /**
     * 检查数据库是否存在
     * 注意：databaseName 已经过验证，只包含字母、数字、下划线和连字符
     */
    private boolean checkDatabaseExists(Statement statement, String databaseName) throws Exception {
        // 验证数据库名称只包含安全字符
        if (!databaseName.matches("^[a-zA-Z0-9_\\-]+$")) {
            throw new IllegalArgumentException("数据库名称包含非法字符: " + databaseName);
        }
        
        String sql = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + databaseName + "'";
        try (ResultSet rs = statement.executeQuery(sql)) {
            return rs.next();
        }
    }
    
    /**
     * 创建数据库
     * 注意：databaseName 已经过验证，只包含字母、数字、下划线和连字符
     */
    private void createDatabase(Statement statement, String databaseName) throws Exception {
        // 验证数据库名称只包含安全字符
        if (!databaseName.matches("^[a-zA-Z0-9_\\-]+$")) {
            throw new IllegalArgumentException("数据库名称包含非法字符: " + databaseName);
        }
        
        // 使用反引号包裹数据库名称，防止与MySQL关键字冲突
        // 注意：MySQL 5.7.8+ 才支持 COMMENT，为了兼容性，先不使用 COMMENT
        String sql = String.format(
            "CREATE DATABASE IF NOT EXISTS `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
            databaseName
        );
        statement.executeUpdate(sql);
        log.info("数据库创建成功: {}", databaseName);
    }
}
