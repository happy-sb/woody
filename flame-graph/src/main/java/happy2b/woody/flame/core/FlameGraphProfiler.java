package happy2b.woody.flame.core;

import happy2b.woody.flame.common.constant.ProfilingResourceType;
import happy2b.woody.flame.tool.graph.FlameGraph;
import happy2b.woody.flame.tool.jni.AsyncProfiler;
import happy2b.woody.flame.common.dto.ProfilingSample;
import happy2b.woody.flame.common.dto.ProfilingSampleBase;
import happy2b.woody.flame.resource.ResourceMethodManager;
import happy2b.woody.flame.resource.fetch.inst.ResourcesExtractor;
import happy2b.woody.flame.tool.ProfilingSampleProcessor;
import happy2b.woody.api.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        ResourcesExtractor.addCustomResource(resource, type, method, idGenerator);
    }

    public static void startProfiling(Map<String, Long> eventIntervals, ProfilingResourceType... types) throws Exception {
//        JNIResourceFetcher.INSTANCE.bootstrap(types);
        if (ResourceMethodManager.PROFILING_INCLUDE_METHODS.isEmpty()) {
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

        Thread.sleep(60 * 1000);

        asyncProfiler.stop();
        TraceManager.stopTracing();

        String dumpTraces = asyncProfiler.dumpTraces(0);

        String[] split = dumpTraces.split(System.lineSeparator());

        List<ProfilingSample> profilingSamples = ProfilingSampleProcessor.parseProfilingSamples(split);
        ProfilingSampleProcessor.populateTracingInfo(profilingSamples, TraceManager.profilingTraces);

        FlameGraph.convert(profilingSamples, "/Users/jiangjibo/Downloads/cpu_flameGraph.html");

        Map<String, ProfilingSampleBase> sampleBaseMap = ProfilingSampleProcessor.extractSampleBase(profilingSamples);

        System.out.println("profilingSamples: " + profilingSamples);

        log.info(dumpTraces);

    }

    public static List finishProfiling() {
        return Collections.emptyList();
    }

    public static boolean allocProfilingEnable() {
        return false;
    }
}
