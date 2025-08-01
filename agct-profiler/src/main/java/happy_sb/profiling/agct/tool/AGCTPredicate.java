package happy_sb.profiling.agct.tool;

import happy_sb.profiler.util.ignore.IgnoredTypesBuilder;
import happy_sb.profiler.util.ignore.IgnoredTypesBuilderImpl;
import happy_sb.profiler.util.ignore.IgnoredTypesPredicate;
import happy_sb.profiling.agct.constant.ProfilingResourceType;
import happy_sb.profiling.agct.core.AGCTProfilerManager;

import static happy_sb.profiling.agct.resource.ResourceMethodManager.TRACING_METHODS;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/23
 */
public class AGCTPredicate {

    private static final IgnoredTypesPredicate TYPES_PREDICATE;

    static {
        IgnoredTypesBuilder builder = new IgnoredTypesBuilderImpl();
        for (ProfilingResourceType resourceType : AGCTProfilerManager.resourceTypes) {
            for (String clazz : resourceType.resourceClasses()) {
                builder.allowClass(clazz);
            }
        }
        TYPES_PREDICATE = builder.buildTransformIgnoredPredicate();
    }

    public static boolean acceptResourceFetching(ClassLoader classLoader, String className) {
        return TYPES_PREDICATE.test(classLoader, className);
    }

    public static boolean acceptTracing(ClassLoader classLoader, String className) {
        return classLoader != null && TRACING_METHODS.contains(className);
    }

}
