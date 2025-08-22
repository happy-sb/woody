package happy2b.woody.core.flame.resource;

import happy2b.woody.common.api.id.IdGenerator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/30
 */
public class ResourceMethodManager {

    private static final Set<Integer> GENERATOR_INDEXES = ConcurrentHashMap.newKeySet();

    public static final List<ResourceMethod> ALL_PROFILING_INCLUDE_METHODS = new ArrayList<>();
    public static final List<ResourceMethod> SELECTED_PROFILING_INCLUDE_METHODS = new ArrayList<>();

    public static IdGenerator[] ID_GENERATORS = new IdGenerator[10];
    public static final Set<String> TRACING_METHODS = ConcurrentHashMap.newKeySet();

    public static void addProfilingIncludeMethod(ResourceMethod method) {
        ALL_PROFILING_INCLUDE_METHODS.add(method);
        TRACING_METHODS.add(method.getClazz().getName().replace(".", "/"));

        int order = method.getIdGenerator().getOrder();
        if (GENERATOR_INDEXES.contains(order)) {
            return;
        }
        if (GENERATOR_INDEXES.size() == ID_GENERATORS.length) {
            refreshIdGenerator();
        }
        addIdGenerator(method.getIdGenerator());
    }

    public static ResourceMethod findProfilingIncludeMethod(String className, String methodName, String descriptor) {
        for (ResourceMethod method : ALL_PROFILING_INCLUDE_METHODS) {
            if (method.getClazz().getName().equals(className) && method.getMethodName().equals(methodName) && method.getSignature().equals(descriptor)) {
                return method;
            }
        }
        return null;
    }

    private static synchronized void refreshIdGenerator() {
        IdGenerator[] generators = new IdGenerator[ID_GENERATORS.length * 2];
        int i = 0;
        for (IdGenerator idGenerator : ID_GENERATORS) {
            generators[i++] = idGenerator;
        }
        ID_GENERATORS = generators;
    }

    private static synchronized void addIdGenerator(IdGenerator idGenerator) {
        if (GENERATOR_INDEXES.add(idGenerator.getOrder())) {
            ID_GENERATORS[GENERATOR_INDEXES.size() - 1] = idGenerator;
        }
    }

    public static Map<String, String> buildResourceTypeMappings() {
        Map<String, String> mappings = new HashMap<>();
        for (ResourceMethod method : ALL_PROFILING_INCLUDE_METHODS) {
            mappings.put(method.getResource(), method.getResourceType());
        }
        return mappings;
    }

    public static Map<String, String> buildMethodPathResourceMappings() {
        Map<String, String> mappings = new HashMap<>();
        for (ResourceMethod method : ALL_PROFILING_INCLUDE_METHODS) {
            mappings.put(method.getMethodPath(), method.getResource());
        }
        return mappings;
    }

    public static Set<ResourceMethod> getResourceByType(String resourceType) {
        Set<ResourceMethod> methods = new HashSet<>();
        for (ResourceMethod includeMethod : ALL_PROFILING_INCLUDE_METHODS) {
            if (includeMethod.getResourceType().equals(resourceType)) {
                methods.add(includeMethod);
            }
        }
        return methods;
    }

    public static Set<ResourceMethod> getSelectedResourceByType(String resourceType) {
        Set<ResourceMethod> methods = new HashSet<>();
        for (ResourceMethod includeMethod : SELECTED_PROFILING_INCLUDE_METHODS) {
            if (includeMethod.getResourceType().equals(resourceType)) {
                methods.add(includeMethod);
            }
        }
        return methods;
    }

    public static void addSelectedResourceMethod(ResourceMethod resourceMethod) {
        SELECTED_PROFILING_INCLUDE_METHODS.add(resourceMethod);
    }

    public static void addSelectedResourceMethod(Collection<ResourceMethod> resourceMethods) {
        SELECTED_PROFILING_INCLUDE_METHODS.addAll(resourceMethods);
    }

}
