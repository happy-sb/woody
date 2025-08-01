package happy_sb.profiling.agct.core;

import happy_sb.profiling.agct.constant.ProfilingEvent;
import happy_sb.profiling.agct.constant.ProfilingResourceType;
import happy_sb.profiling.agct.resource.ResourcesExtractor;
import happy_sb.profiling.api.id.IdGenerator;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/17
 */
public class AGCTProfiler {

    /**
     * add profiler method
     *
     * @param resource    GET /demo
     * @param type        http
     * @param method      com.xxx.xxx.DemoController#getDemo
     * @param idGenerator generate id
     */
    public static void addProfilingResource(String resource, String type, Method method, IdGenerator idGenerator) {
        ResourcesExtractor.addCustomResource(resource, type, method, idGenerator);
    }

    public static void startProfiling(Map<ProfilingEvent, String> events, ProfilingResourceType... types) {
        AGCTProfilerManager.resourceTypes = types;
    }

    public static List finishProfiling() {
        return Collections.emptyList();
    }

    public static boolean allocProfilingEnable() {
        return false;
    }
}
