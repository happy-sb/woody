package happy2b.woody.core.flame.core;

import happy2b.woody.common.api.id.IdGenerator;
import happy2b.woody.core.flame.resource.ResourceMethod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/30
 */
public class ResourceMethodManager {

    public static ResourceMethodManager INSTANCE = new ResourceMethodManager();

    private Set<Integer> GENERATOR_INDEXES = ConcurrentHashMap.newKeySet();

    public List<ResourceMethod> allProfilingIncludeMethods = new ArrayList<>();
    public List<ResourceMethod> selectedProfilingIncludeMethods = new ArrayList<>();

    public IdGenerator[] ID_GENERATORS = new IdGenerator[10];
    public Set<String> TRACING_METHODS = ConcurrentHashMap.newKeySet();

    private ResourceMethodManager() {
    }

    public void addProfilingIncludeMethod(ResourceMethod method) {
        allProfilingIncludeMethods.add(method);
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

    public ResourceMethod findProfilingIncludeMethod(String className, String methodName, String descriptor) {
        for (ResourceMethod method : allProfilingIncludeMethods) {
            if (method.getClazz().getName().equals(className) && method.getMethodName().equals(methodName) && method.getSignature().equals(descriptor)) {
                return method;
            }
        }
        return null;
    }

    private synchronized void refreshIdGenerator() {
        IdGenerator[] generators = new IdGenerator[ID_GENERATORS.length * 2];
        int i = 0;
        for (IdGenerator idGenerator : ID_GENERATORS) {
            generators[i++] = idGenerator;
        }
        ID_GENERATORS = generators;
    }

    private synchronized void addIdGenerator(IdGenerator idGenerator) {
        if (GENERATOR_INDEXES.add(idGenerator.getOrder())) {
            ID_GENERATORS[GENERATOR_INDEXES.size() - 1] = idGenerator;
        }
    }

    public Map<String, String> buildResourceTypeMappings() {
        Map<String, String> mappings = new HashMap<>();
        for (ResourceMethod method : allProfilingIncludeMethods) {
            mappings.put(method.getResource(), method.getResourceType());
        }
        return mappings;
    }

    public Map<String, String> buildMethodPathResourceMappings() {
        Map<String, String> mappings = new HashMap<>();
        for (ResourceMethod method : allProfilingIncludeMethods) {
            mappings.put(method.getMethodPath(), method.getResource());
        }
        return mappings;
    }

    public Set<ResourceMethod> getResourceByType(String resourceType) {
        Set<ResourceMethod> methods = new HashSet<>();
        for (ResourceMethod includeMethod : allProfilingIncludeMethods) {
            if (includeMethod.getResourceType().equals(resourceType)) {
                methods.add(includeMethod);
            }
        }
        return methods;
    }

    public Set<ResourceMethod> getSelectedResourceByType(String resourceType) {
        Set<ResourceMethod> methods = new HashSet<>();
        for (ResourceMethod includeMethod : selectedProfilingIncludeMethods) {
            if (includeMethod.getResourceType().equals(resourceType)) {
                methods.add(includeMethod);
            }
        }
        return methods;
    }

    public void addSelectedResourceMethod(ResourceMethod resourceMethod) {
        selectedProfilingIncludeMethods.add(resourceMethod);
    }

    public void addSelectedResourceMethod(Collection<ResourceMethod> resourceMethods) {
        selectedProfilingIncludeMethods.addAll(resourceMethods);
    }

    public void clearSelectedResource() {
        selectedProfilingIncludeMethods.clear();
    }

}
