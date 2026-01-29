"""
Prometheus指标定义
性能监控：定义HTTP请求相关的指标和系统资源指标
"""
import os
import time
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

# 系统资源指标（Gauge）- Prometheus 标准指标
# process_cpu_seconds_total: 进程累计CPU使用时间（秒）
process_cpu_seconds_total = Counter(
    'process_cpu_seconds_total',
    'Total user and system CPU time spent in seconds'
)

# process_resident_memory_bytes: 进程常驻内存使用量（字节）
process_resident_memory_bytes = Gauge(
    'process_resident_memory_bytes',
    'Resident memory size in bytes'
)

# 系统资源指标（Gauge）- 自定义指标
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

# 初始化进程资源监控
try:
    import psutil
    PSUTIL_AVAILABLE = True
    _process = psutil.Process(os.getpid())
    _last_cpu_times = _process.cpu_times()
    _last_update_time = time.time()
except ImportError:
    PSUTIL_AVAILABLE = False
    _process = None
    _last_cpu_times = None
    _last_update_time = None


def update_process_metrics():
    """
    更新进程资源指标
    这个函数需要定期调用（例如每5秒）
    """
    if not PSUTIL_AVAILABLE or _process is None:
        return
    
    try:
        # 更新内存指标
        memory_info = _process.memory_info()
        process_resident_memory_bytes.set(memory_info.rss)
        
        # 更新CPU指标（累计值）
        global _last_cpu_times, _last_update_time
        current_time = time.time()
        current_cpu_times = _process.cpu_times()
        
        if _last_cpu_times is not None:
            # 计算时间差
            time_delta = current_time - _last_update_time
            if time_delta > 0:
                # 计算CPU使用时间增量
                cpu_delta = (current_cpu_times.user + current_cpu_times.system) - \
                           (_last_cpu_times.user + _last_cpu_times.system)
                # 累加到 Counter
                process_cpu_seconds_total.inc(cpu_delta)
        
        _last_cpu_times = current_cpu_times
        _last_update_time = current_time
    except Exception:
        # 忽略错误，避免影响服务运行
        pass
