package happy_sb.profiling.agct.tool;

import happy_sb.profiler.util.ignore.IgnoredTypesBuilder;
import happy_sb.profiler.util.ignore.IgnoredTypesBuilderImpl;
import happy_sb.profiler.util.ignore.IgnoredTypesPredicate;

import static happy_sb.profiling.agct.asm.ResourceMethodFetcherAdvice.*;
import static happy_sb.profiling.agct.tool.ProfilingIncludeMethods.TRACING_METHODS;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/23
 */
public class AGCTPredicate {

    private static final IgnoredTypesPredicate TYPES_PREDICATE;

    static {
        IgnoredTypesBuilder builder = new IgnoredTypesBuilderImpl();
        builder.allowClass(SPRING_WEB_INSTRUMENTATION_CLASS);
        builder.allowClass(DUBBO_INSTRUMENTATION_CLASS);
        builder.allowClass(GRPC_INSTRUMENTATION_CLASS);
        builder.allowClass(ROCKETMQ_INSTRUMENTATION_CLASS_1);
        builder.allowClass(ROCKETMQ_INSTRUMENTATION_CLASS_2);
        builder.allowClass(KAFKA_INSTRUMENTATION_CLASS);
        TYPES_PREDICATE = builder.buildTransformIgnoredPredicate();
    }

    public static boolean acceptResourceFetching(ClassLoader classLoader, String className) {
        return TYPES_PREDICATE.test(classLoader, className);
    }

    public static boolean acceptTracing(ClassLoader classLoader, String className) {
        return classLoader != null && TRACING_METHODS.contains(className);
    }

}
