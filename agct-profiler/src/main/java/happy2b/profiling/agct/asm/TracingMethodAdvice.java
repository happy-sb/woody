package happy2b.profiling.agct.asm;

import happy2b.profiler.util.reflection.ReflectionUtils;
import happy2b.profiling.agct.core.AGCTTraceManager;
import happy2b.profiling.agct.resource.ResourceMethodManager;
import happy2b.profiling.agct.trace.ProfilingSpan;
import happy2b.profiling.agct.trace.ProfilingTrace;

import java.lang.reflect.Method;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/24
 */
public class TracingMethodAdvice {

    public static final String ADVICE_CLASS = TracingMethodAdvice.class.getName().replace(".", "/");
    public static final String PROFILING_TRACE_CLASS = ProfilingTrace.class.getName().replace(".", "/");

    public static final Method START_TRACE_METHOD;
    public static final Method START_SPAN_METHOD;
    public static final Method FINISH_TRACE_METHOD;

    static {
        START_TRACE_METHOD = ReflectionUtils.findMethod(TracingMethodAdvice.class, "startTrace", String.class, String.class, String.class, int.class);
        START_SPAN_METHOD = ReflectionUtils.findMethod(TracingMethodAdvice.class, "startSpan", String.class, String.class, int.class);
        FINISH_TRACE_METHOD = ReflectionUtils.findMethod(ProfilingTrace.class, "finish");
    }

    public static ProfilingTrace startTrace(String resourceType, String resource, String methodPath, int generatorIndex) {
        return AGCTTraceManager.startProfilingTrace(Thread.currentThread().getId(), resource, resourceType, methodPath, ResourceMethodManager.ID_GENERATORS[generatorIndex].generateTraceId());
    }

    public static ProfilingSpan startSpan(String operationName, String methodPath, int generatorIndex) {
        return AGCTTraceManager.startProfilingSpan(Thread.currentThread().getId(), ResourceMethodManager.ID_GENERATORS[generatorIndex].generateSpanId(), System.nanoTime(), operationName);
    }


}
