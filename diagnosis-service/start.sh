#!/bin/bash
# 诊断服务启动脚本（Linux/Mac）
# 性能优化：配置JVM内存参数

export JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
mvn spring-boot:run

