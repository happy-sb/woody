package happy_sb.profiling.agct.core;

import happy_sb.profiling.agct.tool.ProfilingIncludeMethod;
import happy_sb.profiling.agct.tool.ProfilingSpan;
import happy_sb.profiling.agct.tool.ProfilingTrace;
import happy_sb.profiling.api.AGCTProfiler;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/17
 */
public class AGCTProfilerProcessor implements AGCTProfiler {

    @Override
    public void addProfilingResource(String type, Method method) {
        AGCTProfilerManager.addProfilingIncludeMethod(new ProfilingIncludeMethod(type, method));
    }

    @Override
    public ProfilingTrace startProfilingTrace(Long tid, String resource, String type, String traceId, long time) {
        return AGCTProfilerManager.startProfilingTrace(tid, resource, type, traceId, time);
    }

    @Override
    public ProfilingSpan startProfilingSpan(Long tid, long spanId, long time, String operationName) {
        return AGCTProfilerManager.startProfilingSpan(tid, spanId, time, operationName);
    }


    @Override
    public void startProfiling(Map<String, String> events) {

    }

    @Override
    public List finishProfiling() {
        return Collections.emptyList();
    }

    @Override
    public boolean allocProfilingEnable() {
        return false;
    }
}
