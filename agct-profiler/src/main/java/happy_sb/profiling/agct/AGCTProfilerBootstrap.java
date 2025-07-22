package happy_sb.profiling.agct;

import happy_sb.profiler.util.bytecode.InstrumentationUtils;
import happy_sb.profiling.agct.asm.ResourceFetcherTransformer;

import java.lang.instrument.Instrumentation;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/22
 */
public class AGCTProfilerBootstrap {
    public static void premain(String agentArgs, Instrumentation inst) {
        InstrumentationUtils.setInstrumentation(inst);
        inst.addTransformer(new ResourceFetcherTransformer());
    }
}
