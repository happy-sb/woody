package happy2b.woody.core.tool;

import happy2b.woody.core.flame.core.ResourceMethodManager;


/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/23
 */
public class AGCTPredicate {
    public static boolean acceptTracing(ClassLoader classLoader, String className) {
        return classLoader != null && ResourceMethodManager.INSTANCE.TRACING_METHODS.contains(className);
    }

}
