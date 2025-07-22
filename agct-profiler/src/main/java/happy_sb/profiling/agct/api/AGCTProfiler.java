package happy_sb.profiling.agct.api;

import happy_sb.profiling.agct.tool.ProfilingSpan;
import happy_sb.profiling.agct.tool.ProfilingTrace;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public interface AGCTProfiler {

    AtomicReference<AGCTProfiler> INSTANCE_REFERENCE = new AtomicReference<>();

    /**
     * 添加profiling include方法
     *
     * @param type   资源类型，比如http, dubbo, kafka, rocketmq, grpc等
     * @param method
     */
    void addProfilingResource(String type, Method method);

    /**
     * 启动profiling trace
     *
     * @param tid       线程 id
     * @param resource  资源名称，比如/path/to/resource, dubbo接口全称等
     * @param type http-server, dubbo-server, kafka-consume, rocket-consume等
     * @param traceId   标记当前请求的唯一标识
     * @param time      当前时间
     * @return
     */
    ProfilingTrace startProfilingTrace(Long tid, String resource, String type, String traceId, long time);

    /**
     * 启动profiling span， 非必须
     *
     * @param tid
     * @param spanId
     * @param time
     * @param operationName mysql,redis,http-client, dubbo-client, kafka-produce, rocket-produce等
     * @return
     */
    ProfilingSpan startProfilingSpan(Long tid, long spanId, long time, String operationName);

    /**
     * 分析时间，
     * key为cpu,alloc,wall,lock
     * value为对应的采集频率和单位，
     * cpu: 10ms
     * alloc: 1mb
     * wall: 50ms
     * lock: 50ms
     *
     * @param events
     */
    void startProfiling(Map<String, String> events);

    List finishProfiling();

    boolean allocProfilingEnable();

}
