package happy2b.woody.flame.resource.fetch;

import happy2b.woody.util.reflection.ReflectionUtils;
import happy2b.woody.flame.resource.fetch.inst.ResourcesExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/21
 */
public class ResourceMethodFetchTool {

    private static final Logger log = LoggerFactory.getLogger(ResourceMethodFetchTool.class);

    public static void fetchSpringWebResources(Map<?, ?> handlerMethods) {
        try {
            for (Map.Entry<?, ?> entry : handlerMethods.entrySet()) {
                Object mappingInfo = entry.getKey();
                String path = mappingInfo.toString();
                path = path.substring(1, path.length() - 1);
                if (!path.contains("/")) {
                    path += " /";
                }
                Object handlerMethod = entry.getValue();
                Method method = ReflectionUtils.get(handlerMethod, "method");
                ResourcesExtractor.extractHttpResources(path, method);
            }
        } catch (Exception e) {
            log.error("One-Profiler: Fetch http profiling resource occur exception", e);
        }
    }

    public static void fetchDubboResources(Class type, Object obj) {
        ResourcesExtractor.extractApacheDubboResources(type, obj.getClass().getName());
    }

    public static void fetchGrpcResources(Object bindableService) {
        Object definition = ReflectionUtils.invoke(bindableService, "bindService");
        if (definition == null) {
            return;
        }
        Collection methodDefinitions = ReflectionUtils.invoke(definition, "getMethods");
        for (Object methodDefinition : methodDefinitions) {
            Object descriptor = ReflectionUtils.invoke(methodDefinition, "getMethodDescriptor");
            String fullName = ReflectionUtils.invoke(descriptor, "getFullMethodName");
            Class type = bindableService.getClass();
            ResourcesExtractor.extractGrpcResources(fullName, type);
        }
    }

    public static void fetchRocketMQResources_1(Object container, Object listener) {
        String topic = ReflectionUtils.invoke(container, "getTopic");
        Class<?> type = listener.getClass();
        ResourcesExtractor.extractRocketMQResource(topic, type, "onMessage");
    }

    public static void fetchRocketMQResources_2(Object consumer, Object listener) {
        Object innerConsumer = ReflectionUtils.get(consumer, "defaultMQPushConsumerImpl");
        Object rebalance = ReflectionUtils.invoke(innerConsumer, "getRebalanceImpl");
        Map<String, ?> topics = ReflectionUtils.invoke(rebalance, "getSubscriptionInner");
        ResourcesExtractor.extractRocketMQResource(
                String.join(",", topics.keySet()), listener.getClass(), "consumeMessage");
    }

    public static void fetchKafkaResource(Annotation kafkaListener, Method method) {
        String[] topics = null;
        if (kafkaListener instanceof Proxy && ReflectionUtils.existsField(kafkaListener, "h")) {
            Object handler = ReflectionUtils.get(kafkaListener, "h");
            if (handler != null && ReflectionUtils.existsField(handler, "memberValues")) {
                Map<String, Object> values = ReflectionUtils.get(handler, "memberValues");
                topics = (String[]) values.get("topics");
            }
        }
        if (topics == null) {
            return;
        }
        ResourcesExtractor.extractKafkaResource(String.join(",", topics), method);
    }


}
