package happy2b.woody.agct.tool;

import happy2b.woody.util.ignore.IgnoredTypesBuilder;
import happy2b.woody.util.ignore.IgnoredTypesBuilderImpl;
import happy2b.woody.util.ignore.IgnoredTypesPredicate;
import happy2b.woody.agct.constant.ProfilingResourceType;

import static happy2b.woody.agct.resource.ResourceMethodManager.TRACING_METHODS;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/23
 */
public class AGCTPredicate {

    private static final IgnoredTypesPredicate TYPES_PREDICATE;

    public static ProfilingResourceType[] resourceTypes = new ProfilingResourceType[]
            {
                    ProfilingResourceType.HTTP,
                    ProfilingResourceType.DUBBO,
                    ProfilingResourceType.GRPC,
                    ProfilingResourceType.ROCKETMQ,
                    ProfilingResourceType.KAFKA
            };

    static {
        IgnoredTypesBuilder builder = new IgnoredTypesBuilderImpl();
        for (ProfilingResourceType resourceType : resourceTypes) {
            for (String clazz : resourceType.instResourceClasses()) {
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
