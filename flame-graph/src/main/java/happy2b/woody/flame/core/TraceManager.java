package happy2b.woody.flame.core;

import happy2b.woody.flame.common.constant.ProfilingResourceType;
import happy2b.woody.flame.common.dto.ProfilingSpan;
import happy2b.woody.flame.common.dto.ProfilingTrace;
import happy2b.woody.flame.common.dto.ProfilingTraces;
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

    public static Map<Long, ProfilingTraces> profilingTraces = new ConcurrentHashMap<>();

    static boolean tracingEnabled = false;

    static final Map<String, String> RE_TYPE_THREAD_GROUPS = new ConcurrentHashMap<>();
    static final Map<String, Integer> THREAD_GROUP_RS_STACK_FRAME_HEIGHT_MAP = new ConcurrentHashMap<>();

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();


    public static void startTracing() {
        tracingEnabled = true;
    }

    public static void stopTracing() {
        tracingEnabled = false;
    }

    public static ProfilingTrace startProfilingTrace(Long tid, String resource, String type, String methodPath, Object traceId) {
        if (tracingEnabled) {
            ProfilingTrace profilingTrace = new ProfilingTrace(resource, type, methodPath, traceId);
            profilingTraces.computeIfAbsent(tid, thread1 -> new ProfilingTraces()).startProfilingTrace(profilingTrace);
            return profilingTrace;
        }
        if (!RE_TYPE_THREAD_GROUPS.containsKey(type)) {
            processThreadGroupName(type, tid, methodPath);
        }
        return ProfilingTrace.NO_OP;
    }

    public static ProfilingSpan startProfilingSpan(Long tid, Object spanId, long time, String operationName) {
        if (!profilingTraces.containsKey(tid)) {
            return null;
        }
        return profilingTraces.get(tid).startProfilingSpan(spanId, time, operationName);
    }

    static Map<Long, Integer> collectResourceThreadIdAndStackFrameHeight(ProfilingResourceType... types) throws InterruptedException {
        int i = 0;
        while (RE_TYPE_THREAD_GROUPS.size() < types.length) {
            if (i++ > 30) {
                break;
            }
            Thread.sleep(1 * 1000);
        }
        if (RE_TYPE_THREAD_GROUPS.size() < types.length) {
            for (ProfilingResourceType type : types) {
                if (!RE_TYPE_THREAD_GROUPS.containsKey(type.getValue())) {
                    log.error("Failed to find resource type '{}' threads!", type.getValue());
                }
            }
        }
        Map<Long, Integer> tidHeightMap = new HashMap<>(128);
        for (ThreadInfo threadInfo : THREAD_MX_BEAN.getThreadInfo(THREAD_MX_BEAN.getAllThreadIds())) {
            String threadGroupName = extractThreadGroupName(threadInfo.getThreadName());
            Integer height = THREAD_GROUP_RS_STACK_FRAME_HEIGHT_MAP.get(threadGroupName);
            if (height != null) {
                tidHeightMap.put(threadInfo.getThreadId(), height);
            }
        }
        return tidHeightMap;
    }

    private static void processThreadGroupName(String type, Long tid, String methodPath) {
        ThreadInfo threadInfo = THREAD_MX_BEAN.getThreadInfo(tid, Integer.MAX_VALUE);
        String threadGroupName = extractThreadGroupName(threadInfo.getThreadName());

        RE_TYPE_THREAD_GROUPS.put(type, threadGroupName);

        StackTraceElement[] stackTrace = threadInfo.getStackTrace();
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();
            String methodName = element.getMethodName();
            if (methodPath.startsWith(className) && methodPath.endsWith(methodName)) {
                THREAD_GROUP_RS_STACK_FRAME_HEIGHT_MAP.put(threadGroupName, stackTrace.length - i);
                break;
            }
        }
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
