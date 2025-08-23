package happy2b.woody.core.flame.core;

import happy2b.woody.common.api.id.IdGenerator;
import happy2b.woody.core.flame.common.constant.ProfilingResourceType;
import happy2b.woody.core.flame.common.dto.ProfilingSample;
import happy2b.woody.core.flame.resource.ResourceMethod;
import happy2b.woody.core.tool.ProfilingSampleProcessor;
import happy2b.woody.core.tool.jni.AsyncProfiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/17
 */
public class FlameGraphProfiler {

    private static final Logger log = LoggerFactory.getLogger(FlameGraphProfiler.class);

    /**
     * add profiler method
     *
     * @param resource    GET /demo
     * @param type        http
     * @param method      com.xxx.xxx.DemoController#getDemo
     * @param idGenerator generate id
     */
    public static void addProfilingResource(String resource, String type, Method method, IdGenerator idGenerator) {
        ResourceMethodManager.INSTANCE.addProfilingIncludeMethod(new ResourceMethod(type, resource, method, idGenerator));
    }

    public static void startProfiling(Map<String, Long> eventIntervals, ProfilingResourceType... types) throws Throwable {
        if (ResourceMethodManager.INSTANCE.allProfilingIncludeMethods.isEmpty()) {
            throw new IllegalStateException("No profiling include methods");
        }
        AsyncProfiler asyncProfiler = AsyncProfiler.getInstance();
        Set<String> supportEvents = asyncProfiler.getSupportEvents();
        for (String event : eventIntervals.keySet()) {
            if (event.equals("cpu")) {
                continue;
            }
            if (!supportEvents.contains(event)) {
                throw new IllegalStateException("Event " + event + " is not supported");
            }
        }
        Map<Long, Integer> tidRSFrameHeightMap = TraceManager.collectResourceThreadIdAndStackFrameHeight(types);
        if (tidRSFrameHeightMap.isEmpty()) {
            throw new IllegalStateException("No resource thread found");
        }
        asyncProfiler.syncTidRsStackFrameHeightMap(tidRSFrameHeightMap);
        TraceManager.startTracing();
        asyncProfiler.start(eventIntervals);
    }

    public static List<ProfilingSample> finishProfiling() throws Throwable {
        TraceManager.stopTracing();
        AsyncProfiler asyncProfiler = AsyncProfiler.getInstance();
        asyncProfiler.stop();
        String dumpTraces = asyncProfiler.dumpTraces(0);
        String[] split = dumpTraces.split(System.lineSeparator());
        return ProfilingSampleProcessor.parseProfilingSamples(split);
    }

    public static boolean allocProfilingEnable() {
        return false;
    }
}
