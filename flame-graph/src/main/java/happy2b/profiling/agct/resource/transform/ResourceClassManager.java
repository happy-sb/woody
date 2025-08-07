package happy2b.profiling.agct.resource.transform;

import happy2b.profiler.util.AgentThreadFactory;
import happy2b.profiler.util.bytecode.InstrumentationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/30
 */
public class ResourceClassManager {

    public static final ResourceClassManager INSTANCE = new ResourceClassManager();

    private static final Logger log = LoggerFactory.getLogger(ResourceClassManager.class);

    private static final Set<Class> resourceClasses = ConcurrentHashMap.newKeySet();

    private static final LinkedBlockingDeque<Class> PENDING_TRANSFORM_METHODS = new LinkedBlockingDeque<>();

    private static AtomicBoolean transformerAdded = new AtomicBoolean(false);

    private static Thread transformWorker;

    private ResourceClassManager() {
    }

    public void addResourceClass(Class clazz) {
        if (!resourceClasses.add(clazz)) {
            return;
        }
        if (transformerAdded.compareAndSet(false, true)) {
            InstrumentationUtils.getInstrumentation().addTransformer(new ResourceMethodTransformer(), true);
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
                }
            }
        });
        transformWorker.start();
    }

}
