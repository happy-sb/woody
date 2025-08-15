package happy2b.woody.flame;

import happy2b.woody.util.bytecode.InstrumentationUtils;
import happy2b.woody.flame.common.constant.ProfilingResourceType;
import happy2b.woody.flame.tool.jni.AsyncProfiler;
import happy2b.woody.flame.resource.fetch.jni.JNIResourceFetcher;

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
        JNIResourceFetcher.INSTANCE.bootstrap(ProfilingResourceType.GRPC);
        AsyncProfiler.getInstance();
    }
}
