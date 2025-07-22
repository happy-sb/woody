package happy_sb.profiling.inst;



import happy_sb.profiler.util.bytecode.InstrumentationUtils;
import happy_sb.profiling.inst.api.InstrumentationProfiler;
import happy_sb.profiling.inst.core.InstrumentationProfilingProcessor;

import java.lang.instrument.Instrumentation;

/**
 * 探针启动器
 *
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/10
 */
public class InstrumentationProfilerBootstrap {
    public static void premain(String agentArgs, Instrumentation inst) {
        InstrumentationUtils.setInstrumentation(inst);
//        inst.addTransformer(new ResourceFetcherTransformer());
        InstrumentationProfiler.INSTANCE_REFERENCE.set(new InstrumentationProfilingProcessor());
    }
}