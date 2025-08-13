package happy2b.woody.agct.core;

import happy2b.woody.agct.constant.ProfilingResourceType;
import happy2b.woody.agct.trace.ProfilingSpan;
import happy2b.woody.agct.trace.ProfilingTrace;
import happy2b.woody.agct.trace.ProfilingTraces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/31
 */
public class TraceManager {

    private static final Logger log = LoggerFactory.getLogger(TraceManager.class);
    private static Map<Long, ProfilingTraces> profilingTraces = new ConcurrentHashMap<>();

    static boolean tracingEnabled = false;

    private static final Map<String, String> RESOURCE_TYPE_THREAD_GROUPS = new ConcurrentHashMap<>();

    static final ProfilingTrace NO_OP = new ProfilingTrace("NO_OP", null, null, 0);

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    public static final Map<Long, Integer> TID_RS_STACK_FRAME_DEEP_MAP = new HashMap<>(128);

    public static void startTracing() {
        tracingEnabled = true;
    }

    public static ProfilingTrace startProfilingTrace(Long tid, String resource, String type, String methodPath, Object traceId) {
        if (tracingEnabled) {
            ProfilingTrace profilingTrace = new ProfilingTrace(resource, type, methodPath, traceId);
            profilingTraces.computeIfAbsent(tid, thread1 -> new ProfilingTraces()).startProfilingTrace(profilingTrace);
            return profilingTrace;
        }
        if (!RESOURCE_TYPE_THREAD_GROUPS.containsKey(type)) {
            RESOURCE_TYPE_THREAD_GROUPS.put(type, extractThreadGroupName(tid));
        }
        return NO_OP;
    }

    public static ProfilingSpan startProfilingSpan(Long tid, Object spanId, long time, String operationName) {
        if (!profilingTraces.containsKey(tid)) {
            return null;
        }
        return profilingTraces.get(tid).startProfilingSpan(spanId, time, operationName);
    }

    static List<Long> collectResourceThreadIds(ProfilingResourceType... types) throws InterruptedException {
        int i = 0;
        while (RESOURCE_TYPE_THREAD_GROUPS.size() < types.length) {
            if (i++ > 30) {
                break;
            }
            Thread.sleep(1 * 1000);
        }
        if (RESOURCE_TYPE_THREAD_GROUPS.size() < types.length) {
            for (ProfilingResourceType type : types) {
                if (!RESOURCE_TYPE_THREAD_GROUPS.containsKey(type.getValue())) {
                    log.error("Failed to find resource type '{}' threads!", type.getValue());
                }
            }
        }

        List<Long> result = new ArrayList<>();
        Collection<String> threadGroups = RESOURCE_TYPE_THREAD_GROUPS.values();
        for (ThreadInfo threadInfo : THREAD_MX_BEAN.getThreadInfo(THREAD_MX_BEAN.getAllThreadIds())) {
            if (threadGroups.contains(extractThreadGroupName(threadInfo.getThreadName()))) {
                result.add(threadInfo.getThreadId());
            }
        }
        return result;
    }

    private static String extractThreadGroupName(Long tid) {
        ThreadInfo threadInfo = THREAD_MX_BEAN.getThreadInfo(tid);
        return extractThreadGroupName(threadInfo.getThreadName());
    }

    private static String extractThreadGroupName(String threadName) {
        int end = -1;
        for (int i = threadName.length() - 1; i >= 0; i--) {
            char c = threadName.charAt(i);
            if (c >= '0' && c <= '9') {
                end = i;
                if (i > 1 && threadName.charAt(i - 1) == '-') {
                    end = i - 1;
                    break;
                }
            } else {
                break;
            }
        }
        if (end == -1) {
            return threadName;
        }
        return threadName.substring(0, end).trim();
    }

}
