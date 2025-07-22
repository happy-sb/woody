package happy_sb.profiling.instrument;

import happy_sb.profiling.api.InstrumentationProfiler;
import happy_sb.profiling.instrument.stats.MethodProfilingManager;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/16
 */
public class ProfilingTest {

    @Test
    public void testInstrumentation() throws Throwable {
        Method method = ProfilingTool.class.getDeclaredMethod("methodB0");

        InstrumentationProfiler profiler = InstrumentationProfiler.INSTANCE_REFERENCE.get();
        boolean profiling = profiler.profiling(method);
        for (int i = 0; i < 1000; i++) {
            new ProfilingTool().methodB0();
            Thread.sleep(10 * 1000);
            System.out.println("1111111");
            if (i % 6 == 0) {
                MethodProfilingManager.nextDuration();
            }
        }
    }
}
