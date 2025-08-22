package happy2b.woody.inst.advice;

import happy2b.woody.common.utils.MethodUtil;
import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.common.api.Config;
import happy2b.woody.common.api.MethodIntrospection;
import happy2b.woody.inst.stats.MethodProfilingManager;
import happy2b.woody.inst.tools.ProfilingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

public class ProfilingAdvice {

    private static final Logger log = LoggerFactory.getLogger(ProfilingAdvice.class);

    private static final ThreadLocal<Stack<ProfilingEntity>> ENTITY_LOCAL_CACHE = ThreadLocal.withInitial(() -> new Stack<>());

    private static final Config config = Config.get();

    // 当前执行的方法 >> 内省出的需要增强的方法
    private static final Map<Method, Set<Method>> needTransformMethods = new ConcurrentHashMap<>();
    private static final Map<Method, Set<String>> introspectFields = new ConcurrentHashMap<>();


    private static final int entity_length = 9;
    private static final ThreadLocal<ProfilingEntity[]> ENTITIES = ThreadLocal.withInitial(() -> {
        ProfilingEntity[] objects = new ProfilingEntity[entity_length];
        for (int i = 0; i < entity_length; i++) {
            objects[i] = new ProfilingEntity();
        }
        return objects;
    });

    private static final int MAX_RESOURCE_COST_ENTRY_NUM = 100;
    private static final ThreadLocal<long[][]> longArrayPool2 = ThreadLocal.withInitial(
            () -> {
                long[][] ls = new long[MAX_RESOURCE_COST_ENTRY_NUM + 1][];
                ls[0] = new long[1];
                return ls;
            }
    );

    private static final int RESOURCE_CALCULATE_MEMORY_ALLOC = 48;
    private static final int RESOURCE_CALCULATE_CPU_COST = 1000;
    private static final int RESOURCE_CALCULATE_TIME_COST = 1000;

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();


    public static final Method ON_METHOD_ENTER;
    public static final Method STATIC_ON_METHOD_ENTER;
    public static final Method START_PROFILING;
    public static final Method FINISH_PROFILING;
    public static final Method ON_METHOD_EXIT;

    static {
        ON_METHOD_ENTER = ReflectionUtils.findMethod(ProfilingAdvice.class, "onMethodEnter", Object.class, String.class, Object[].class);
        STATIC_ON_METHOD_ENTER = ReflectionUtils.findMethod(ProfilingAdvice.class, "onMethodEnter", Method.class);
        ON_METHOD_EXIT = ReflectionUtils.findMethod(ProfilingAdvice.class, "onMethodExit", ProfilingEntity.class);
        START_PROFILING = ReflectionUtils.findMethod(ProfilingAdvice.class, "startProfiling", int.class, ProfilingEntity.class);
        FINISH_PROFILING = ReflectionUtils.findMethod(ProfilingAdvice.class, "finishProfiling", int.class, ProfilingEntity.class);
    }

    public static ProfilingEntity onMethodEnter(Object thiz, String method, Object[] params) {
        MethodIntrospection introspection;
        try {
            Method targetMethod = ReflectionUtils.findMethod(thiz.getClass(), method, params);
            introspection = config.getMethodLineIntrospection(targetMethod);
            if (!introspection.isVisitInfoHandled()) {
                introspection.handleVisitInfos(thiz);
            }
            ProfilingEntity entity = borrowEntity();
            if (entity == null) {
                return null;
            }
            entity.tid = Thread.currentThread().getId();
            entity.introspection = introspection;
//            ENTITY_LOCAL_CACHE.get().push(entity);

            snapshotResource(entity);
            return entity;
        } catch (Throwable throwable) {
            log.error("Execute profiling method before occur exception, method: {}#{}", thiz.getClass().getName(), method, throwable);
            return null;
        }
    }

    public static ProfilingEntity onMethodEnter(Method method) {
        try {
            MethodIntrospection introspection = config.getMethodLineIntrospection(method);
            if (!introspection.isVisitInfoHandled()) {
                introspection.handleVisitInfos(null);
            }
            ProfilingEntity entity = borrowEntity();
            if (entity == null) {
                return null;
            }
            entity.tid = Thread.currentThread().getId();
            entity.introspection = introspection;
//            ENTITY_LOCAL_CACHE.get().push(entity);
            snapshotResource(entity);
            return entity;
        } catch (Throwable throwable) {
            log.error("Execute profiling method before occur exception, method: {}#{}", method.getDeclaringClass().getName(), method, throwable);
            return null;
        }
    }

    public static void startProfiling(final int startLine, final ProfilingEntity entity) {
        if (entity == null) {
            return;
        }
        MethodIntrospection introspection = entity.introspection;
        try {
            int[] previousBlock = introspection.previousAllocBlocks(startLine);
            if (previousBlock != null) {
                List<long[]> resourceCosts = introspection.getResourceCostContainer();
                if (resourceCosts == null || resourceCosts.size() > MAX_RESOURCE_COST_ENTRY_NUM) {
                    return;
                }
                resourceCosts.add(calculateResourceCost(entity, previousBlock[0], previousBlock[1]));
            }
        } catch (Exception e) {
            log.error("Execute profiling before for method:{}#{} occur exception", introspection.getMethod().getDeclaringClass().getName(), introspection.getMethod().getName(), e);
        }
    }

    public static void finishProfiling(final int endLine, final ProfilingEntity entity) {
        if (entity == null) {
            return;
        }
        MethodIntrospection introspection = entity.introspection;
        try {
            List<long[]> resourceCosts = introspection.getResourceCostContainer();
            if (resourceCosts == null || resourceCosts.size() > MAX_RESOURCE_COST_ENTRY_NUM) {
                return;
            }
            int startLine = introspection.findStartLine(endLine);
            resourceCosts.add(calculateResourceCost(entity, startLine, endLine, 0, 0, 0));
        } catch (Exception e) {
            log.error("Execute profiling after for method:{}#{} occur exception", introspection.getMethod().getDeclaringClass().getName(), introspection.getMethod().getName(), e);
        }
    }

    public static void onMethodExit(final ProfilingEntity entity) {
        if (entity == null) {
            return;
        }
        MethodIntrospection introspection = entity.introspection;
        List<long[]> longs = introspection.getResourceCostContainer();
        if (longs != null && !longs.isEmpty()) {
            MethodProfilingManager.accumulateProfiling(introspection.getMethod(), longs);
            returnLongArray2(longs);
            longs.clear();
        }
        entity.used = false;
    }

    private void extractFieldForExecutingMethod(Object thiz, String field, String name, String desc, String signature) {
        Method method = MethodUtil.findTransformTargetMethod(thiz, field, name, desc);
        if (method != null) {
            ProfilingEntity entity = ENTITY_LOCAL_CACHE.get().pop();
            MethodIntrospection introspection = entity.introspection;
            Method executable = introspection.getMethod();
            needTransformMethods.get(executable).add(method);
            introspectFields.get(executable).add(signature != null ? signature : buildSignature(field, name, desc));
        }
    }

    private String buildSignature(String owner, String name, String desc) {
        return owner + "." + name + desc;
    }

    private static void snapshotResource(ProfilingEntity entity) {
        long startCpuTime = THREAD_MX_BEAN.getCurrentThreadCpuTime();
        entity.cpuSnapshot = startCpuTime;
        entity.timeSnapshot = System.nanoTime();
        if (Config.jmxMemAllocAvailable) {
            long startMemoryAlloc = ((com.sun.management.ThreadMXBean) THREAD_MX_BEAN).getThreadAllocatedBytes(entity.tid);
            entity.memorySnapshot = startMemoryAlloc;
        }
    }

    private static ProfilingEntity borrowEntity() {
        ProfilingEntity[] objects = ENTITIES.get();
        for (int i = 0; i < objects.length; i++) {
            ProfilingEntity entity = objects[i];
            if (!entity.used) {
                entity.used = true;
                return entity;
            }
        }
        return null;
    }

    private static long[] borrowLongArray2() {
        long[][] longs = longArrayPool2.get();
        int index = (int) longs[0][0];
        if (index == MAX_RESOURCE_COST_ENTRY_NUM) {
            return new long[5];
        }
        index++;
        longs[0][0] = index;
        if (longs[index] == null) {
            for (int i = index; i < index + 20; i++) {
                longs[i] = new long[5];
            }
        }
        long[] array = longs[index];
        longs[index] = null;
        return array;
    }

    private static void returnLongArray2(List<long[]> array) {
        long[][] longs = longArrayPool2.get();
        int index = (int) longs[0][0];
        int j = 0;
        while (j < array.size() && index >= 0) {
            longs[index--] = array.get(j++);
            longs[0][0] = index;
        }
    }

    private static long[] calculateResourceCost(ProfilingEntity entity, int startLine, int endLine) {
        return calculateResourceCost(entity, startLine, endLine, 0, 0, 0);
    }

    private static long[] calculateResourceCost(ProfilingEntity entity, int startLine, int endLine, long durationDelta, long cpuDelta, long allocDelta) {

        long[] delta = borrowLongArray2();
        delta[0] = startLine;
        delta[1] = endLine;

        long endCpuTime = THREAD_MX_BEAN.getCurrentThreadCpuTime();
        delta[2] = endCpuTime - entity.cpuSnapshot - cpuDelta - RESOURCE_CALCULATE_CPU_COST;
        entity.cpuSnapshot = endCpuTime;

        if (Config.jmxMemAllocAvailable) {
            long endMemoryAlloc = ((com.sun.management.ThreadMXBean) THREAD_MX_BEAN).getThreadAllocatedBytes(entity.tid);
//      delta[4] = endMemoryAlloc - entity.memorySnapshot - allocDelta - RESOURCE_CALCULATE_MEMORY_ALLOC;
            delta[4] = endMemoryAlloc - entity.memorySnapshot - allocDelta;
            entity.memorySnapshot = endMemoryAlloc;
        }

        long now = System.nanoTime();
        delta[3] = now - entity.timeSnapshot - durationDelta - RESOURCE_CALCULATE_TIME_COST;
        entity.timeSnapshot = now;

        return delta;
    }

}
