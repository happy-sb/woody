package happy_sb.profiling.agct.core;

import happy_sb.profiling.agct.tracing.ProfilingSpan;
import happy_sb.profiling.agct.tracing.ProfilingTrace;
import happy_sb.profiling.agct.tracing.ProfilingTraces;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/31
 */
public class AGCTTraceManager {

    private static volatile Map<Long, ProfilingTraces> profilingTraces = new ConcurrentHashMap<>();

    public static ProfilingTrace startProfilingTrace(Long tid, String resource, String type, String methodPath, Object traceId) {
        ProfilingTrace profilingTrace = new ProfilingTrace(resource, type, methodPath, traceId);
        profilingTraces.computeIfAbsent(tid, thread1 -> new ProfilingTraces()).startProfilingTrace(profilingTrace);
        return profilingTrace;
    }

    public static ProfilingSpan startProfilingSpan(Long tid, Object spanId, long time, String operationName) {
        if (!profilingTraces.containsKey(tid)) {
            return null;
        }
        return profilingTraces.get(tid).startProfilingSpan(spanId, time, operationName);
    }

}
