package happy_sb.profiling.agct.resource;

import happy_sb.profiling.api.id.IdGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/30
 */
public class ResourceMethodManager {

    private static final List<ResourceMethod> PROFILING_INCLUDE_METHODS = new ArrayList<>();
    private static final Set<Integer> GENERATOR_INDEXES = ConcurrentHashMap.newKeySet();

    public static IdGenerator[] ID_GENERATORS = new IdGenerator[10];
    public static final Set<String> TRACING_METHODS = ConcurrentHashMap.newKeySet();

    public static void addProfilingIncludeMethod(ResourceMethod method) {
        PROFILING_INCLUDE_METHODS.add(method);
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
        for (ResourceMethod method : PROFILING_INCLUDE_METHODS) {
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

}
