package happy2b.profiling.agct.resource.inst;

import happy2b.profiler.util.AgentThreadFactory;
import happy2b.profiler.util.bytecode.InstrumentationUtils;
import happy2b.profiling.agct.asm.TracingMethodTransformer;
import happy2b.profiling.agct.core.AGCTProfiler;
import happy2b.profiling.agct.resource.IResourceFetcher;
import happy2b.profiling.agct.resource.ResourceMethod;
import happy2b.profiling.agct.resource.ResourceMethodManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/30
 */
public class InstResourceFetcher implements IResourceFetcher {

    public static final IResourceFetcher INSTANCE = new InstResourceFetcher();

    private static final Logger log = LoggerFactory.getLogger(InstResourceFetcher.class);

    private static final LinkedBlockingDeque<Class> PENDING_TRANSFORM_METHODS = new LinkedBlockingDeque<>();

    private static AtomicBoolean transformerAdded = new AtomicBoolean(false);

    private static Thread transformWorker;

    public void bootstrap() {
        InstrumentationUtils.getInstrumentation().addTransformer(new ResourceFetcherTransformer());
    }

    @Override
    public void transformTracingMethod(ResourceMethod method) {
        ResourceMethodManager.addProfilingIncludeMethod(method);
        Class<?> clazz = method.getMethod().getDeclaringClass();
        if (clazz.getName().startsWith("org.springframework")) {
            return;
        }
        addTracingClass(clazz);
    }

    private void addTracingClass(Class clazz) {
        if (transformerAdded.compareAndSet(false, true)) {
            InstrumentationUtils.getInstrumentation().addTransformer(new TracingMethodTransformer(), true);
            startTransformThread();
        }
        try {
            PENDING_TRANSFORM_METHODS.put(clazz);
        } catch (InterruptedException e) {
            log.error("One-Profiler: Put clazz to pending transform methods failed!", e);
        }
    }


    private void startTransformThread() {
        transformWorker = AgentThreadFactory.newAgentThread(AgentThreadFactory.AgentThread.TRACE_METHOD_TRANSFORMER, new Runnable() {
            @Override
            public void run() {
                Class clazz;
                while (true) {
                    try {
                        clazz = PENDING_TRANSFORM_METHODS.take();
                    } catch (InterruptedException e) {
                        log.error("One-Profiler: Take class from pending transform methods failed!", e);
                        return;
                    }
                    try {
                        InstrumentationUtils.getInstrumentation().retransformClasses(clazz);
                    } catch (Exception e) {
                        log.error("One-Profiler: Retransform class '{}' occur exception!", clazz.getName(), e);
                    }

                    AGCTProfiler.startProfiling(null, null);
                }
            }
        });
        transformWorker.start();
    }

}
