@echo off
REM 诊断服务启动脚本（Windows）
REM 性能优化：配置JVM内存参数

set JAVA_OPTS=-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
mvn spring-boot:run

