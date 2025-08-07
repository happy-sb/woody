package happy2b.profiling.agct.core;

import happy2b.profiling.agct.constant.ProfilingResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/21
 */
public class AGCTProfilerManager {

    private static final Logger log = LoggerFactory.getLogger(AGCTProfilerManager.class);

    private static boolean perfEnabled;
    private static boolean allocEnabled;

    public static ProfilingResourceType[] resourceTypes = new ProfilingResourceType[]
            {
                    ProfilingResourceType.HTTP,
                    ProfilingResourceType.DUBBO,
                    ProfilingResourceType.GRPC,
                    ProfilingResourceType.ROCKETMQ,
                    ProfilingResourceType.KAFKA
            };

    public static Map<Long, Integer> threadIdResourceFrameHitHeight = new HashMap<>(128);
    private Map<Long, Integer> threadTraceNum = Collections.EMPTY_MAP;

    private AtomicInteger profilingCount = new AtomicInteger(0);

    public static List<Long> activityOrderedThreadIds = new ArrayList<>();

    public static boolean isPerfEnabled() {
        return perfEnabled;
    }

    public static boolean isAllocEnabled() {
        return allocEnabled;
    }


}
