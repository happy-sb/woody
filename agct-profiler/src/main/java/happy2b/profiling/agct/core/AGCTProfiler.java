package happy2b.profiling.agct.core;

import happy2b.profiling.agct.constant.ProfilingEvent;
import happy2b.profiling.agct.constant.ProfilingResourceType;
import happy2b.profiling.agct.resource.ResourcesExtractor;
import happy2b.profiling.api.id.IdGenerator;
import happy2b.profiling.agct.jni.AsyncProfiler;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        AsyncProfiler asyncProfiler = AsyncProfiler.getInstance();

        Set<String> supportEvents = asyncProfiler.getSupportEvents();

        asyncProfiler.start("alloc", 100);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        asyncProfiler.stop();
        String traces = asyncProfiler.dumpTraces(10000);
        System.out.println(traces);
    }

    public static List finishProfiling() {
        return Collections.emptyList();
    }

    public static boolean allocProfilingEnable() {
        return false;
    }
}
