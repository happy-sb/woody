package happy_sb.profiling.agct.tool;

import happy_sb.profiler.util.AgentThreadFactory;
import happy_sb.profiler.util.bytecode.InstrumentationUtils;
import happy_sb.profiling.agct.asm.TracingMethodTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/30
 */
public class TracingMethodTrigger {

    private static final Logger log = LoggerFactory.getLogger(TracingMethodTrigger.class);

    private static final LinkedBlockingDeque<Class[]> PENDING_TRANSFORM_METHODS = new LinkedBlockingDeque<>();

    private static AtomicBoolean transformerAdded = new AtomicBoolean(false);

    private static Thread transformWorker;

    public static void addTracingClass(Class[] classes) {
        if (transformerAdded.compareAndSet(false, true)) {
            InstrumentationUtils.getInstrumentation().addTransformer(new TracingMethodTransformer(), true);
            startTransformThread();
        }
        try {
            PENDING_TRANSFORM_METHODS.put(classes);
        } catch (InterruptedException e) {
            log.error("One-Profiler: Put clazz to pending transform methods failed!", e);
        }
    }

    private static void startTransformThread() {
        transformWorker = AgentThreadFactory.newAgentThread(AgentThreadFactory.AgentThread.TRACE_METHOD_TRANSFORMER, new Runnable() {
            @Override
            public void run() {
                Class[] classes;
                while (true) {
                    try {
                        classes = PENDING_TRANSFORM_METHODS.take();
                    } catch (InterruptedException e) {
                        log.error("One-Profiler: Take class from pending transform methods failed!", e);
                        return;
                    }
                    try {
                        InstrumentationUtils.getInstrumentation().retransformClasses(classes);
                    } catch (Exception e) {
                        log.error("One-Profiler: Retransform class '{}' occur exception!", classes[0].getName(), e);
                    }
                }
            }
        });
        transformWorker.start();
    }

}
