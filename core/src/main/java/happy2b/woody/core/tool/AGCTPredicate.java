package happy2b.woody.core.tool;

import static happy2b.woody.core.flame.resource.ResourceMethodManager.TRACING_METHODS;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/23
 */
public class AGCTPredicate {
    public static boolean acceptTracing(ClassLoader classLoader, String className) {
        return classLoader != null && TRACING_METHODS.contains(className);
    }

}
