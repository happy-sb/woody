package happy2b.profiling.inst;



import happy2b.profiler.util.bytecode.InstrumentationUtils;
import happy2b.profiling.api.InstrumentationProfiler;
import happy2b.profiling.inst.core.InstrumentationProfilingProcessor;

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
        InstrumentationProfiler.INSTANCE_REFERENCE.set(new InstrumentationProfilingProcessor());
    }
}