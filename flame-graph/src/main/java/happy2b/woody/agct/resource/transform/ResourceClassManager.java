package happy2b.woody.agct.resource.transform;

import happy2b.woody.util.bytecode.InstrumentationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/30
 */
public class ResourceClassManager {

    public static final ResourceClassManager INSTANCE = new ResourceClassManager();

    private static final Logger log = LoggerFactory.getLogger(ResourceClassManager.class);

    private static final Set<Class> RESOURCE_CLASSES = ConcurrentHashMap.newKeySet();

    private static AtomicBoolean transformerAdded = new AtomicBoolean(false);

    private ResourceClassManager() {
    }

    public void addResourceClass(Class clazz) {
        RESOURCE_CLASSES.add(clazz);
    }

    public void retransformResourceClasses() {
        if (transformerAdded.compareAndSet(false, true)) {
            InstrumentationUtils.getInstrumentation().addTransformer(new ResourceMethodTransformer(), true);
        }
        startTransformThread();
    }

    private void startTransformThread() {
        for (Class clazz : RESOURCE_CLASSES) {
            try {
                InstrumentationUtils.getInstrumentation().retransformClasses(clazz);
            } catch (Throwable e) {
                log.error("One-Profiler: Retransform class '{}' occur exception!", clazz.getName(), e);
            }
        }
        RESOURCE_CLASSES.clear();
    }
}
