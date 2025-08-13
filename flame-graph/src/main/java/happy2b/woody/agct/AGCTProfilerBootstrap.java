package happy2b.woody.agct;

import happy2b.woody.util.bytecode.InstrumentationUtils;
import happy2b.woody.agct.constant.ProfilingResourceType;
import happy2b.woody.agct.jni.AsyncProfiler;
import happy2b.woody.agct.resource.fetch.jni.JNIResourceFetcher;

import java.lang.instrument.Instrumentation;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/22
 */
public class AGCTProfilerBootstrap {
    public static void premain(String agentArgs, Instrumentation inst) {
        InstrumentationUtils.setInstrumentation(inst);
//        InstResourceFetcher.INSTANCE.bootstrap();
        JNIResourceFetcher.INSTANCE.bootstrap(ProfilingResourceType.DUBBO, ProfilingResourceType.GRPC, ProfilingResourceType.HTTP, ProfilingResourceType.KAFKA, ProfilingResourceType.ROCKETMQ);
        AsyncProfiler.getInstance();
    }
}
