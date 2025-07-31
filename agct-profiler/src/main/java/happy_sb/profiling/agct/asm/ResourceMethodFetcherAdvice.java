package happy_sb.profiling.agct.asm;

import happy_sb.profiler.util.reflection.ReflectionUtils;
import happy_sb.profiling.agct.core.AGCTProfilerManager;
import happy_sb.profiling.agct.tool.TracingMethodTrigger;
import happy_sb.profiling.agct.tool.TracingResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/21
 */
public class ResourceMethodFetcherAdvice {

    private static final Logger log = LoggerFactory.getLogger(ResourceMethodFetcherAdvice.class);

    public static final String FETCHER_ADVICE_CLASS = ResourceMethodFetcherAdvice.class.getName().replace(".", "/");

    public static final Method SPRING_WEB_FETCHER_METHOD;
    public static final String SPRING_WEB_INSTRUMENTATION_METHOD = "handlerMethodsInitialized";
    public static final String SPRING_WEB_INSTRUMENTATION_CLASS = "org.springframework.web.servlet.handler.AbstractHandlerMethodMapping".replace(".", "/");

    public static final Method DUBBO_FETCHER_METHOD;
    public static final String DUBBO_INSTRUMENTATION_METHOD = "doExport";
    public static final String DUBBO_INSTRUMENTATION_CLASS = "org.apache.dubbo.config.ServiceConfig".replace(".", "/");

    public static final Method GRPC_FETCHER_METHOD;
    public static final String GRPC_INSTRUMENTATION_METHOD_1 = "intercept";
    public static final String GRPC_INSTRUMENTATION_METHOD_2 = "interceptForward";
    public static final String GRPC_INSTRUMENTATION_CLASS = "io.grpc.ServerInterceptors".replace(".", "/");

    public static final Method ROCKETMQ_FETCHER_METHOD_1;
    public static final String ROCKETMQ_INSTRUMENTATION_METHOD_1_1 = "setRocketMQListener";
    public static final String ROCKETMQ_INSTRUMENTATION_METHOD_1_2 = "setRocketMQReplyListener";
    public static final String ROCKETMQ_INSTRUMENTATION_CLASS_1 = "org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer".replace(".", "/");

    public static final Method ROCKETMQ_FETCHER_METHOD_2;
    public static final String ROCKETMQ_INSTRUMENTATION_METHOD_2_1 = "setMessageListener";
    public static final String ROCKETMQ_INSTRUMENTATION_METHOD_2_2 = "registerMessageListener";
    public static final String ROCKETMQ_INSTRUMENTATION_CLASS_2 = "org.apache.rocketmq.client.consumer.DefaultMQPushConsumer".replace(".", "/");

    public static final Method KAFKA_FETCHER_METHOD;
    public static final String KAFKA_INSTRUMENTATION_METHOD = "processKafkaListener";
    public static final String KAFKA_INSTRUMENTATION_CLASS = "org.springframework.kafka.annotation.KafkaListenerAnnotationBeanPostProcessor".replace(".", "/");

    static {
        SPRING_WEB_FETCHER_METHOD = ReflectionUtils.findMethod(ResourceMethodFetcherAdvice.class, "fetchSpringWebProfilingResources", Map.class);
        DUBBO_FETCHER_METHOD = ReflectionUtils.findMethod(ResourceMethodFetcherAdvice.class, "fetchDubboProfilingResource", Class.class, Object.class);
        GRPC_FETCHER_METHOD = ReflectionUtils.findMethod(ResourceMethodFetcherAdvice.class, "fetchGrpcProfilingResource", Object.class);
        ROCKETMQ_FETCHER_METHOD_1 = ReflectionUtils.findMethod(ResourceMethodFetcherAdvice.class, "fetchRocketMQProfilingResources_1", Object.class, Object.class);
        ROCKETMQ_FETCHER_METHOD_2 = ReflectionUtils.findMethod(ResourceMethodFetcherAdvice.class, "fetchRocketMQProfilingResources_2", Object.class, Object.class);
        KAFKA_FETCHER_METHOD = ReflectionUtils.findMethod(ResourceMethodFetcherAdvice.class, "fetchKafkaProfilingResource", Annotation.class, Method.class);
    }

    public static void fetchSpringWebProfilingResources(Map<?, ?> handlerMethods) {
        try {
            Set<Class> classes = new HashSet<>();
            for (Map.Entry<?, ?> entry : handlerMethods.entrySet()) {
                Object mappingInfo = entry.getKey();
                String path = mappingInfo.toString();
                path = path.substring(1, path.length() - 1);
                if (!path.contains("/")) {
                    path += " /";
                }
                Object handlerMethod = entry.getValue();
                Method method = ReflectionUtils.get(handlerMethod, "method");
                AGCTProfilerManager.getProfilingResources().addHttpResources(path, method);
                Class<?> clazz = method.getDeclaringClass();
                if (!clazz.getName().startsWith("org.springframework")) {
                    classes.add(clazz);
                }
            }
            TracingMethodTrigger.addTracingClass(classes.toArray(new Class[0]));
        } catch (Exception e) {
            log.error("One-Profiler: Fetch http profiling resource occur exception", e);
        }
    }

    public static void fetchDubboProfilingResource(Class type, Object obj) {
        AGCTProfilerManager.getProfilingResources().addApacheDubboResources(type, obj.getClass().getName());
    }

    public static void fetchGrpcProfilingResource(Object bindableService) {
        Object definition = ReflectionUtils.invoke(bindableService, "bindService");
        if (definition == null) {
            return;
        }
        Collection methodDefinitions = ReflectionUtils.invoke(definition, "getMethods");
        for (Object methodDefinition : methodDefinitions) {
            Object descriptor = ReflectionUtils.invoke(methodDefinition, "getMethodDescriptor");
            String fullName = ReflectionUtils.invoke(descriptor, "getFullMethodName");
            Class type = bindableService.getClass();
            AGCTProfilerManager.getProfilingResources().addGrpcResources(fullName, type);
        }
    }

    public static void fetchRocketMQProfilingResources_1(Object container, Object listener) {
        String topic = ReflectionUtils.invoke(container, "getTopic");
        Class<?> type = listener.getClass();
        AGCTProfilerManager.getProfilingResources().addRocketMQResource(topic, type, "onMessage");
    }

    public static void fetchRocketMQProfilingResources_2(Object consumer, Object listener) {
        Object innerConsumer = ReflectionUtils.get(consumer, "defaultMQPushConsumerImpl");
        Object rebalance = ReflectionUtils.invoke(innerConsumer, "getRebalanceImpl");
        Map<String, ?> topics = ReflectionUtils.invoke(rebalance, "getSubscriptionInner");
        AGCTProfilerManager.getProfilingResources().addRocketMQResource(
                String.join(",", topics.keySet()), listener.getClass(), "consumeMessage");
    }

    public static void fetchKafkaProfilingResource(Annotation kafkaListener, Method method) {
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
        TracingResources profilingResources = AGCTProfilerManager.getProfilingResources();
        profilingResources.addKafkaResource(String.join(",", topics), method);
    }


}
