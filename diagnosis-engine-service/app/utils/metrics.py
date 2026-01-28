"""
Prometheus指标定义
性能监控：定义HTTP请求相关的指标
"""
from prometheus_client import Counter, Histogram, Gauge, REGISTRY

# HTTP请求总数（Counter）
http_requests_total = Counter(
    'http_requests_total',
    'Total HTTP requests',
    ['method', 'path', 'status']
)

# HTTP请求耗时（Histogram）
http_request_duration_seconds = Histogram(
    'http_request_duration_seconds',
    'HTTP request duration in seconds',
    ['method', 'path'],
    buckets=[0.1, 0.5, 1.0, 2.0, 5.0, 10.0, 30.0]  # 自定义分桶
)

# HTTP请求错误数（Counter）
http_request_errors_total = Counter(
    'http_request_errors_total',
    'Total HTTP request errors',
    ['method', 'path', 'error_type']
)

# 系统资源指标（Gauge）
system_cpu_usage_percent = Gauge(
    'system_cpu_usage_percent',
    'System CPU usage percentage',
    ['service']
)

system_memory_usage_bytes = Gauge(
    'system_memory_usage_bytes',
    'System memory usage in bytes',
    ['service']
)

