package happy_sb.profiling.agct.asm;

import happy_sb.profiling.agct.core.AGCTProfilerManager;
import happy_sb.profiling.agct.tool.ProfilingResources;
import happy_sb.profiling.utils.reflection.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

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

    static {
        SPRING_WEB_FETCHER_METHOD = ReflectionUtils.findMethod(ResourceMethodFetcherAdvice.class, "fetchSpringWebProfilingResources", Map.class);
        DUBBO_FETCHER_METHOD = ReflectionUtils.findMethod(ResourceMethodFetcherAdvice.class, "fetchDubboProfilingResource", Class.class, Object.class);
    }

    public static void fetchSpringWebProfilingResources(Map<?, ?> handlerMethods) {
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
                AGCTProfilerManager.getProfilingResources().addHttpResources(path, method.getClass().getName() + "." + method.getName(), method);
            }
        } catch (Exception e) {
            log.error("Fetch http profiling resource occur exception", e);
        }
    }

    public static void fetchDubboProfilingResource(Class type, Object obj) {
        AGCTProfilerManager.getProfilingResources().addApacheDubboResources(type, obj.getClass().getName());
    }

    public static void fetchGrpcTracingResource(String fullName, Class type) {
        AGCTProfilerManager.getProfilingResources().addGrpcResources(fullName, type);
    }

    public static void fetchRocketMQProfilingResources(String topic, Class type, String method) {
        AGCTProfilerManager.getProfilingResources().addRocketMQResource(topic, type, method);
    }

    public static void fetchKafkaListener(Annotation kafkaListener, Method method) {
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
        ProfilingResources profilingResources = AGCTProfilerManager.getProfilingResources();
        for (String topic : topics) {
            profilingResources.addKafkaResource(topic, method);
        }
    }


}
