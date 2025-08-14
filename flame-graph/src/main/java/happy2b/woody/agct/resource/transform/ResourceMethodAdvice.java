package happy2b.woody.agct.resource.transform;

import happy2b.woody.util.reflection.ReflectionUtils;
import happy2b.woody.agct.core.TraceManager;
import happy2b.woody.agct.resource.ResourceMethodManager;
import happy2b.woody.agct.trace.ProfilingSpan;
import happy2b.woody.agct.trace.ProfilingTrace;
import happy2b.woody.api.id.ParametricIdGenerator;

import java.lang.reflect.Method;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/24
 */
public class ResourceMethodAdvice {

    public static final String ADVICE_CLASS = ResourceMethodAdvice.class.getName().replace(".", "/");
    public static final String PROFILING_TRACE_CLASS = ProfilingTrace.class.getName().replace(".", "/");
    public static final String PROFILING_TRACE_CLASS_DESC = "L" + PROFILING_TRACE_CLASS + ";";

    public static final Method START_TRACE_METHOD;
    public static final Method START_TRACE_WITH_PARAM_METHOD;
    public static final Method START_SPAN_METHOD;
    public static final Method START_SPAN_WITH_PARAM_METHOD;
    public static final Method FINISH_TRACE_METHOD;

    static {
        START_TRACE_METHOD = ReflectionUtils.findMethod(ResourceMethodAdvice.class, "startTrace", String.class, String.class, String.class, int.class);
        START_TRACE_WITH_PARAM_METHOD = ReflectionUtils.findMethod(ResourceMethodAdvice.class, "startTrace", String.class, String.class, String.class, int.class, Object.class);
        START_SPAN_METHOD = ReflectionUtils.findMethod(ResourceMethodAdvice.class, "startSpan", String.class, String.class, int.class);
        START_SPAN_WITH_PARAM_METHOD = ReflectionUtils.findMethod(ResourceMethodAdvice.class, "startSpan", String.class, String.class, int.class, Object.class);
        FINISH_TRACE_METHOD = ReflectionUtils.findMethod(ProfilingTrace.class, "finish");
    }

    public static ProfilingTrace startTrace(String resourceType, String resource, String methodPath, int generatorIndex) {
        return TraceManager.startProfilingTrace(Thread.currentThread().getId(), resource, resourceType, methodPath, ResourceMethodManager.ID_GENERATORS[generatorIndex].generateTraceId());
    }

    public static ProfilingTrace startTrace(String resourceType, String resource, String methodPath, int generatorIndex, Object param) {
        ParametricIdGenerator idGenerator = (ParametricIdGenerator) ResourceMethodManager.ID_GENERATORS[generatorIndex];
        return TraceManager.startProfilingTrace(Thread.currentThread().getId(), resource, resourceType, methodPath, idGenerator.generateTraceId(param));
    }

    public static ProfilingSpan startSpan(String operationName, String methodPath, int generatorIndex) {
        return TraceManager.startProfilingSpan(Thread.currentThread().getId(), ResourceMethodManager.ID_GENERATORS[generatorIndex].generateSpanId(), System.nanoTime(), operationName);
    }

    public static ProfilingSpan startSpan(String operationName, String methodPath, int generatorIndex, Object param) {
        ParametricIdGenerator idGenerator = (ParametricIdGenerator) ResourceMethodManager.ID_GENERATORS[generatorIndex];
        return TraceManager.startProfilingSpan(Thread.currentThread().getId(), idGenerator.generateSpanId(param), System.nanoTime(), operationName);
    }


}
