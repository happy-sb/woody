package happy2b.profiling.agct.resource.fetch.inst;

import happy2b.profiling.agct.resource.ResourceMethod;
import happy2b.profiling.agct.resource.fetch.IResourceFetcher;
import happy2b.profiling.api.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ResourcesExtractor {

    private static final Logger log = LoggerFactory.getLogger(ResourcesExtractor.class);

    private static Map<String, String> resources = new ConcurrentHashMap<>();

    public static void extractHttpResources(String resource, Method method) {
        String methodPath = method.getDeclaringClass().getName() + "." + method.getName();
        if (resources.putIfAbsent(resource, methodPath) == null) {
            IResourceFetcher.addResourceMethod(new ResourceMethod("http", resource, method));
        }
    }

    public static void extractGrpcResources(String fullName, Class type) {
        for (Method method : type.getDeclaredMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 2 && parameterTypes[1].getName().equals("io.grpc.stub.StreamObserver")) {
                String methodPath = type.getName() + "." + method.getName();
                String resource = buildRPCResource(type, method);
                if (resources.containsKey(resource)) {
                    return;
                }
                resources.put(resource, methodPath);
                IResourceFetcher.addResourceMethod(new ResourceMethod("grpc", resource, method));
            }
        }
    }

    public static void extractApacheDubboResources(Class type, String implement) {
        if (implement.startsWith("org.apache.dubbo.") || implement.startsWith("com.alibaba.dubbo.")) {
            return;
        }
        try {
            Class<?> implementClass = type.getClassLoader().loadClass(implement);
            for (Method method : type.getDeclaredMethods()) {
                String methodPath = implement + "." + method.getName();
                String resource = buildRPCResource(type, method);
                if (resources.containsKey(resource)) {
                    return;
                }
                resources.put(resource, methodPath);
                Method m = implementClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
                IResourceFetcher.addResourceMethod(new ResourceMethod("dubbo", resource, m));
            }
        } catch (Exception e) {
            log.error("Load dubbo class failed!", e);
        }
    }


    public static void extractRocketMQResource(String topic, Class type, String method) {
        String typeName = type.getName();
        if (typeName.startsWith("org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer")) {
            return;
        }
        String resource = "Consume Topic " + topic;
        String methodPath = type.getName() + "." + method;
        if (resources.containsKey(resource)) {
            return;
        }
        resources.put(resource, methodPath);
        IResourceFetcher.addResourceMethod(new ResourceMethod("rocketmq", resource, findMostApplicableMethod(type, method)));
    }

    public static void extractKafkaResource(String topic, Method method) {
        String methodPath = method.getDeclaringClass().getName() + "." + method.getName();
        String resource = "Consume Topic " + topic;
        if (resources.containsKey(resource)) {
            return;
        }
        resources.put(resource, methodPath);
        IResourceFetcher.addResourceMethod(new ResourceMethod("kafka", resource, method));
    }

    public static void addCustomResource(String resource, String type, Method method, IdGenerator idGenerator) {
        if (resources.containsKey(resource)) {
            return;
        }
        resources.put(resource, type + "." + method.getName());
        IResourceFetcher.addResourceMethod(new ResourceMethod(type, resource, method, idGenerator));
    }

    private static String getMethodDesc(Method method) {
        String className = method.getDeclaringClass().getName();
        Class<?>[] types = method.getParameterTypes();
        StringBuilder sb = new StringBuilder(className).append(".").append(method.getName()).append("(");
        for (int i = 0; i < types.length; i++) {
            sb.append(types[i].getSimpleName());
            if (i < types.length - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private static String buildRPCResource(Class type, Method method) {
        String[] split = type.getName().split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            if (i < split.length - 1) {
                sb.append(split[i].charAt(0)).append(".");
            } else {
                sb.append(split[i]);
            }
        }
        sb.append("/").append(method.getName());
        return sb.toString();
    }

    private static Method findMostApplicableMethod(Class type, String method) {
        List<Method> namedMethods = new ArrayList<>();
        for (Method declaredMethod : type.getDeclaredMethods()) {
            if (!declaredMethod.getName().equals(method)) {
                continue;
            }
            namedMethods.add(declaredMethod);
        }
        if (namedMethods.isEmpty()) {
            return null;
        }
        if (namedMethods.size() == 1) {
            return namedMethods.get(0);
        }

        namedMethods = namedMethods.stream().filter(new Predicate<Method>() {
            @Override
            public boolean test(Method method) {
                return !(method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Object.class);
            }
        }).collect(Collectors.toList());

        return namedMethods.stream().sorted(new Comparator<Method>() {
            @Override
            public int compare(Method o1, Method o2) {
                // 优先取public方法
                boolean pub1 = Modifier.isPublic(o1.getModifiers());
                boolean pub2 = Modifier.isPublic(o2.getModifiers());
                if (pub1 && !pub2) {
                    return -1;
                }
                if (!pub1 && pub2) {
                    return 1;
                }
                // 优先取参数长的方法
                int l1 = o1.getParameterCount();
                int l2 = o2.getParameterCount();
                return l1 > l2 ? -1 : 1;
            }
        }).findFirst().get();

    }
}
