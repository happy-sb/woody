package happy2b.woody.flame.resource.fetch.jni;

import happy2b.woody.util.bytecode.InstrumentationUtils;
import happy2b.woody.util.reflection.ReflectionUtils;
import happy2b.woody.flame.common.constant.ProfilingEvent;
import happy2b.woody.flame.common.constant.ProfilingResourceType;
import happy2b.woody.flame.core.FlameGraphProfiler;
import happy2b.woody.flame.tool.jni.AsyncProfiler;
import happy2b.woody.flame.resource.ResourceMethod;
import happy2b.woody.flame.resource.ResourceMethodManager;
import happy2b.woody.flame.resource.fetch.IResourceFetcher;
import happy2b.woody.flame.resource.fetch.ResourceMethodFetchTool;
import happy2b.woody.flame.resource.transform.ResourceClassManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static happy2b.woody.flame.resource.fetch.ResourceFetchingConst.*;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/1
 */
public class JNIResourceFetcher implements IResourceFetcher {

    public static final IResourceFetcher INSTANCE = new JNIResourceFetcher();

    private static final Logger log = LoggerFactory.getLogger(JNIResourceFetcher.class);

    @Override
    public void bootstrap(ProfilingResourceType... types) {
        new Thread(() -> {
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            List<String> resourceTypes = new ArrayList<>();
            for (ProfilingResourceType type : types) {
                resourceTypes.addAll(Arrays.asList(type.jniResourceClasses()));
            }

            List<Class> classList = InstrumentationUtils.findClass(resourceTypes.toArray(new String[0]));
            for (Class clazz : classList) {
                if (clazz.getName().equals(SPRING_WEB_FRAMEWORK_CLASS)) {
                    fetchSpringWebResources(clazz);
                } else if (clazz.getName().equals(DUBBO_FRAMEWORK_CLASS)) {
                    fetchDubboResources(clazz);
                } else if (clazz.getName().equals(GRPC_FRAMEWORK_CLASS)) {
                    fetchGrpcResources(clazz);
                } else if (clazz.getName().equals(ROCKETMQ_FRAMEWORK_CLASS_1)) {
                    fetchRocketMQResources_1(clazz);
                } else if (clazz.getName().equals(ROCKETMQ_FRAMEWORK_CLASS_2)) {
                    fetchRocketMQResources_2(clazz);
                } else if (clazz.getName().equals(KAFKA_FRAMEWORK_CLASS)) {
                    fetchKafkaResources(clazz);
                }
            }

            ResourceClassManager.INSTANCE.retransformResourceClasses();

            AsyncProfiler.getInstance().setResourceMethods(ResourceMethodManager.PROFILING_INCLUDE_METHODS);

            Map<String, Long> events = new HashMap<>();
//            events.put(ProfilingEvent.WALL.getSegment(), 50_000_000L);
            events.put(ProfilingEvent.CPU.getSegment(), 5_000_000L);
//            events.put(ProfilingEvent.ALLOC.getSegment(), 500 * 1024L);

            try {
                FlameGraphProfiler.startProfiling(events, types);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

        }).start();

    }

    private void fetchSpringWebResources(Class clazz) {
        try {
            Object[] instances = AsyncProfiler.getInstance().getInstances(clazz, 1);
            if (instances == null || instances.length == 0) {
                log.error("One-Profiler: Failed to fetch spring web '{}' instance!", clazz.getName());
                return;
            }
            Map<?, ?> handlerMethods = ReflectionUtils.invoke(instances[0], "getHandlerMethods");
            ResourceMethodFetchTool.fetchSpringWebResources(handlerMethods);
        } catch (Throwable e) {
            log.error("One-Profiler: Fetch http profiling resource occur exception", e);
        }
    }

    private void fetchDubboResources(Class clazz) {
        try {
            Object[] instances = AsyncProfiler.getInstance().getInstances(clazz, 100);
            if (instances == null || instances.length == 0) {
                log.error("One-Profiler: Failed to fetch dubbo '{}' instance!", clazz.getName());
                return;
            }
            for (Object instance : instances) {
                Class<?> interfaceClass = ReflectionUtils.get(instance, "interfaceClass");
                Object ref = ReflectionUtils.get(instance, "ref");
                ResourceMethodFetchTool.fetchDubboResources(interfaceClass, ref);
            }
        } catch (Throwable e) {
            log.error("One-Profiler: Fetch http profiling resource occur exception", e);
        }
    }

    private void fetchGrpcResources(Class clazz) {
        try {
            Object[] instances = AsyncProfiler.getInstance().getInstances(clazz, 3);
            if (instances == null || instances.length == 0) {
                log.error("One-Profiler: Failed to fetch grpc '{}' instance!", clazz.getName());
                return;
            }
            for (Object instance : instances) {
                List<Object> services = ReflectionUtils.getFieldValues(instance, "registry", "services");
                for (Object service : services) {
                    Map<String, ?> methods = ReflectionUtils.get(service, "methods");
                    for (Map.Entry<String, ?> entry : methods.entrySet()) {
                        String key = entry.getKey();
                        String[] split = key.split("/");
                        Method method = fetchGrpcResourceMethod(split[0], split[1], entry.getValue());
                        if (method == null) {
                            log.error("One-Profiler: Can not find method: " + split[1] + " in " + entry.getValue().getClass().getName());
                            return;
                        }
                        IResourceFetcher.addResourceMethod(new ResourceMethod("grpc", key, method));
                    }
                }
            }
        } catch (Throwable e) {
            log.error("One-Profiler: Fetch http profiling resource occur exception", e);
        }
    }

    private Method fetchGrpcResourceMethod(String clazz, String methodName, Object service) {
        Object serviceImpl = ReflectionUtils.getFieldValues(service, "handler", "callHandler", "method", "serviceImpl");
        if (serviceImpl == null) {
            log.error("One-Profiler: ServiceImpl is null, class: " + clazz);
            return null;
        }
        List<Method> methods = ReflectionUtils.findMethodIgnoreParamTypes(serviceImpl.getClass(), methodName);
        for (Method method : methods) {
            if (method.getDeclaringClass().getName().contains("$") || !Modifier.isPublic(method.getModifiers()) || method.getParameterTypes().length != 2) {
                continue;
            }
            return method;
        }
        return null;
    }

    private void fetchRocketMQResources_1(Class clazz) {
        try {
            Object[] instances = AsyncProfiler.getInstance().getInstances(clazz, 100);
            if (instances == null || instances.length == 0) {
                log.error("One-Profiler: Failed to fetch rocketmq '{}' instance!", clazz.getName());
                return;
            }
            for (Object instance : instances) {
                Object messageListener = ReflectionUtils.invoke(instance, "getRocketMQListener");
                ResourceMethodFetchTool.fetchRocketMQResources_1(instance, messageListener);
            }
        } catch (Throwable e) {
            log.error("One-Profiler: Fetch rocketmq resource occur exception!", e);
        }
    }

    private void fetchRocketMQResources_2(Class clazz) {
        try {
            Object[] instances = AsyncProfiler.getInstance().getInstances(clazz, 100);
            if (instances == null || instances.length == 0) {
                log.error("One-Profiler: Failed to fetch rocketmq '{}' instance!", clazz.getName());
                return;
            }
            for (Object instance : instances) {
                Object messageListener = ReflectionUtils.get(instance, "messageListener");
                if (messageListener == null) {
                    log.error("One-Profiler: Failed to fetch rocketmq '{}' messageListener!", clazz.getName());
                    return;
                }
                ResourceMethodFetchTool.fetchRocketMQResources_2(instance, messageListener);
            }
        } catch (Throwable e) {
            log.error("One-Profiler: Fetch rocketmq resource occur exception!", e);
        }
    }

    private void fetchKafkaResources(Class clazz) {
        try {
            Object[] instances = AsyncProfiler.getInstance().getInstances(clazz, 100);
            if (instances == null || instances.length == 0) {
                log.error("One-Profiler: Failed to fetch kafka '{}' instance!", clazz.getName());
                return;
            }
            for (Object instance : instances) {
                Method method = ReflectionUtils.invoke(instance, "getMethod");
                if (method == null) {
                    log.error("One-Profiler: Failed to fetch kafka '{}' method!", instance.getClass().getName());
                    return;
                }
                Collection<String> topics = ReflectionUtils.invoke(instance, "getTopics");
                ResourceMethod resourceMethod = new ResourceMethod("kafka", "Consume Topic " + String.join(",", topics), method);
                IResourceFetcher.addResourceMethod(resourceMethod);
            }
        } catch (Throwable e) {
            log.error("One-Profiler: Fetch kafka resource occur exception!", e);
        }
    }
}
