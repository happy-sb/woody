package happy2b.profiling.api;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 插桩性能分析器
 * 当参数的方法为字符串时,格式为: 类名#方法名(参数类型全路径)
 *
 * @author jiangjb
 */
public interface InstrumentationProfiler {

    AtomicReference<InstrumentationProfiler> INSTANCE_REFERENCE = new AtomicReference<>();

    boolean profiling(Method method) throws Throwable;

    /**
     * 批量插桩, 确保 {@param methodSet} 为同一个class
     *
     * @param clazz
     * @param methodSet
     * @return
     * @throws Throwable
     */
    boolean profiling(Class clazz, Set<Method> methodSet) throws Throwable;

    Set<Method> getProfilingMethods();

    boolean resetProfiling(Method method) throws Throwable;

    boolean resetProfiling(Class<?> clazz) throws Throwable;

    boolean resetAllProfiling() throws Throwable;

    String getMethodSourceCode(Method method);

    String getMethodProfilingStats(Method method, int latestDurationInMin);


}
