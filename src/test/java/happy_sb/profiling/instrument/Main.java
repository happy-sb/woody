package happy_sb.profiling.instrument;


import happy_sb.profiling.api.InstrumentationProfiler;

import java.lang.reflect.Method;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws Throwable {
        Method method = ProfilingTool.class.getDeclaredMethod("methodB0");

        InstrumentationProfiler profiler = InstrumentationProfiler.INSTANCE_REFERENCE.get();
        boolean profiling = profiler.profiling(method);
        while (true) {
            new ProfilingTool().methodB0();
            Thread.sleep(30 * 1000);
            System.out.println("1111111");
        }
    }
}