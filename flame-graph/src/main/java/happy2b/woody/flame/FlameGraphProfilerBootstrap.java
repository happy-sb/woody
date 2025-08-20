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
public class FlameGraphProfilerBootstrap {

    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        InstrumentationUtils.setInstrumentation(inst);
        System.out.println(agentArgs);
//        InstResourceFetcher.INSTANCE.bootstrap();
        JNIResourceFetcher.INSTANCE.bootstrap(ProfilingResourceType.GRPC, ProfilingResourceType.HTTP, ProfilingResourceType.DUBBO, ProfilingResourceType.ROCKETMQ);
        AsyncProfiler.getInstance();
    }


}
