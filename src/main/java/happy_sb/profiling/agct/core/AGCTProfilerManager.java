package happy_sb.profiling.agct.core;

import happy_sb.profiling.agct.tool.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/21
 */
public class AGCTProfilerManager {

    private static final Logger log = LoggerFactory.getLogger(AGCTProfilerManager.class);

    private static boolean perfEnabled;
    private static boolean allocEnabled;

    private static final ProfilingResources profilingResources = new ProfilingResources();
    private static List<ProfilingIncludeMethod> profilingIncludeMethods = new ArrayList<>();
    public static Map<Long, Integer> threadIdResourceFrameHitHeight = new HashMap<>(128);
    private Map<Long, Integer> threadTraceNum = Collections.EMPTY_MAP;

    private AtomicInteger profilingCount = new AtomicInteger(0);

    public static List<Long> activityOrderedThreadIds = new ArrayList<>();
    private static volatile Map<Long, ProfilingTraces> profilingTraces = new ConcurrentHashMap<>();

    public static ProfilingTrace startProfilingTrace(Long tid, String resource, String type, String traceId, long time) {
        ProfilingTrace profilingTrace = new ProfilingTrace(resource.toString(), type, traceId, time);
        profilingTraces.computeIfAbsent(tid, thread1 -> new ProfilingTraces()).startProfilingTrace(profilingTrace);
        return profilingTrace;
    }

    public static ProfilingSpan startProfilingSpan(Long tid, long spanId, long time, String operationName) {
        if (!profilingTraces.containsKey(tid)) {
            return null;
        }
        return profilingTraces.get(tid).startProfilingSpan(spanId, time, operationName);
    }

    public static boolean isPerfEnabled() {
        return perfEnabled;
    }

    public static boolean isAllocEnabled() {
        return allocEnabled;
    }

    public static void addProfilingIncludeMethod(ProfilingIncludeMethod method) {
        profilingIncludeMethods.add(method);
    }

    public static ProfilingResources getProfilingResources() {
        return profilingResources;
    }
}
