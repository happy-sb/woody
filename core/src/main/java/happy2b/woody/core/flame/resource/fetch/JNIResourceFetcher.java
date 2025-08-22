package happy2b.woody.core.flame.resource.fetch;

import happy2b.woody.core.flame.common.constant.ProfilingEvent;
import happy2b.woody.core.flame.common.constant.ProfilingResourceType;
import happy2b.woody.core.flame.common.dto.ProfilingSample;
import happy2b.woody.core.flame.common.dto.ProfilingSampleBase;
import happy2b.woody.core.flame.core.FlameGraphProfiler;
import happy2b.woody.core.flame.core.TraceManager;
import happy2b.woody.core.flame.resource.ResourceMethodManager;
import happy2b.woody.core.tool.ProfilingSampleProcessor;
import happy2b.woody.core.tool.graph.FlameGraph;
import happy2b.woody.core.tool.jni.AsyncProfiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/1
 */
public class JNIResourceFetcher {


    private void doProfiling(ProfilingResourceType... types) {
        try {
            AsyncProfiler.getInstance().setResourceMethods(ResourceMethodManager.ALL_PROFILING_INCLUDE_METHODS);

            Map<String, Long> events = new HashMap<>();
//            events.put(ProfilingEvent.ALLOC.getSegment(), 500 * 1024L);
            events.put(ProfilingEvent.WALL.getSegment(), 50_000_000L);
            events.put(ProfilingEvent.CPU.getSegment(), 5_000_000L);

            FlameGraphProfiler.startProfiling(events, types);

            Thread.sleep(60 * 1000);

            List<ProfilingSample> profilingSamples = FlameGraphProfiler.finishProfiling();

            ProfilingSampleProcessor.populateTracingInfo(profilingSamples, TraceManager.profilingTraces);
            Map<String, ProfilingSampleBase> sampleBaseMap = ProfilingSampleProcessor.extractSampleBase(profilingSamples);

            Map<String, List<ProfilingSample>> eventSamples = profilingSamples.stream().collect(Collectors.groupingBy(ProfilingSample::getEventType));
            for (Map.Entry<String, List<ProfilingSample>> entry : eventSamples.entrySet()) {
                FlameGraph.convert(entry.getValue(), sampleBaseMap, "/Users/jiangjibo/Downloads/" + entry.getKey() + "_flameGraph.html");
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
