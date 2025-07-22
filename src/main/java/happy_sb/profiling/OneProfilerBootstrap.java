package happy_sb.profiling;


import happy_sb.profiling.agct.asm.ResourceFetcherTransformer;
import happy_sb.profiling.api.InstrumentationProfiler;
import happy_sb.profiling.instrument.core.InstrumentationProfilingProcessor;
import happy_sb.profiling.utils.bytecode.InstrumentationUtils;

import java.lang.instrument.Instrumentation;

/**
 * 探针启动器
 *
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/10
 */
public class OneProfilerBootstrap {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("动态Agent启动，参数: " + agentArgs);
        InstrumentationUtils.setInstrumentation(inst);
        inst.addTransformer(new ResourceFetcherTransformer());
        InstrumentationProfiler.INSTANCE_REFERENCE.set(new InstrumentationProfilingProcessor());
    }
}