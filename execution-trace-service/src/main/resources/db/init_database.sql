-- ============================================
-- 创建执行追踪服务数据库
-- ============================================
-- 使用方法：在MySQL客户端执行此脚本
-- mysql -u root -p < init_database.sql
-- 或者在MySQL Workbench、Navicat等工具中执行

CREATE DATABASE IF NOT EXISTS aidoctor_trace 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci
    COMMENT 'AI医生系统执行追踪服务数据库';

-- 使用数据库
USE aidoctor_trace;

-- 注意：表结构会由Flyway自动创建（V1__init_execution_trace_table.sql）

