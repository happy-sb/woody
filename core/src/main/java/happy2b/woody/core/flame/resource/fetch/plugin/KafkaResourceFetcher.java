package happy2b.woody.core.flame.resource.fetch.plugin;

import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.core.flame.common.constant.ProfilingResourceType;
import happy2b.woody.core.flame.resource.ResourceMethod;
import happy2b.woody.core.flame.resource.fetch.ResourceFetcher;
import happy2b.woody.core.tool.jni.AsyncProfiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/21
 */
public class KafkaResourceFetcher implements ResourceFetcher {

    private static final Logger log = LoggerFactory.getLogger(KafkaResourceFetcher.class);

    public static final KafkaResourceFetcher INSTANCE = new KafkaResourceFetcher();

    private KafkaResourceFetcher() {
    }

    @Override
    public String[] getResourceClassName() {
        return ProfilingResourceType.KAFKA.getResourceClasses();
    }

    @Override
    public void fetchResources(Class clazz) {
        try {
            Object[] instances = AsyncProfiler.getInstance().getInstances(clazz, 100);
            if (instances == null || instances.length == 0) {
                log.error("Woody: Failed to fetch kafka '{}' instance!", clazz.getName());
                return;
            }
            for (Object instance : instances) {
                Method method = ReflectionUtils.invoke(instance, "getMethod");
                if (method == null) {
                    log.error("Woody: Failed to fetch kafka '{}' method!", instance.getClass().getName());
                    return;
                }
                Collection<String> topics = ReflectionUtils.invoke(instance, "getTopics");
                addResourceMethod(new ResourceMethod("kafka", "Consume Topic " + String.join(",", topics), method));
            }
        } catch (Throwable e) {
            log.error("Woody: Fetch kafka resource occur exception!", e);
        }
    }

    @Override
    public ProfilingResourceType resourceType() {
        return ProfilingResourceType.KAFKA;
    }

}
