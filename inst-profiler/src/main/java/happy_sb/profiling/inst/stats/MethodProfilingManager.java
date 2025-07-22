package happy_sb.profiling.inst.stats;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MethodProfilingManager {

    private static int index;
    private static final Map<Method, MethodProfiler> PROFILER_MAP = new ConcurrentHashMap<>();
    public static final Set<Method> PENDING_RETRANSFORM_METHODS = new HashSet<>();

    public static int profiling_cpu_threshold = Integer.parseInt(System.getProperty("inst.profiling.cpuThreshold", "200000"));
    public static int profiling_alloc_threshold = Integer.parseInt(System.getProperty("inst.profiling.allocThreshold", "1000"));
    public static int deep_introspect_resource_cost_percent_threshold = Integer.parseInt(System.getProperty("inst.profiling.deepIntrospectThreshold", "50"));

    public static final int segment_length = 11;
    private static final String[] blanks = new String[]{"", " ", "  ", "   ", "    ", "     ", "      ", "       ", "        "};
    public static final String stats_title = "  行号/周期     执行次数    累计cpu      平均cpu     累计内存      平均内存     累计耗时     平均耗时";

    public static void nextDuration() {
        if (PROFILER_MAP.isEmpty()) {
            return;
        }
        index++;
        for (Map.Entry<Method, MethodProfiler> profilerEntry : PROFILER_MAP.entrySet()) {
            profilerEntry.getValue().refresh(index);
        }
    }

    public static String getProfilingStats(Method method, int duration) {
        if (PROFILER_MAP.isEmpty()) {
            return null;
        }
        return PROFILER_MAP.get(method).getProfilingStats(index, duration);
    }

    public static void accumulateProfiling(Method method, List<long[]> profiling) {
        PROFILER_MAP.computeIfAbsent(method, m -> new MethodProfiler(m, index)).compilationProfiling(index, profiling);
    }

    public static String formatProfiling(String formatLine, long ts, double cu, double mu, double du) {
        return formatString(
                "|", segment_length, formatLine, ts + "",
                formatTime(cu), formatTime(cu / ts),
                formatAlloc(mu), formatAlloc(mu / ts),
                formatTime(du), formatTime(du / ts));
    }

    public static String formatString(String splitter, int segmentLen, String... segments) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (i == segments.length - 1) {
                sb.append(blanks[1]).append(segment);
                break;
            }
            if (segment.length() >= segmentLen) {
                sb.append(segment);
            } else {
                int delta = segmentLen - segment.length();
                if (delta == 1) {
                    sb.append(segment);
                } else {
                    int half = delta / 2;
                    int mod = delta % 2;
                    sb.append(blanks[half]).append(segment).append(blanks[half]).append(blanks[mod]);
                }
            }
            if (i < segments.length - 1) {
                sb.append(splitter);
            }
        }
        return sb.toString();
    }

    public static String formatTime(double v) {
        return format(v, "ns", "us", "ms", "s");
    }

    public static String formatAlloc(double v) {
        return format(v, "b", "kb", "mb", "gb");
    }

    private static String format(double v, String u1, String u2, String u3, String u4) {
        if (v < 1000) {
            return Math.round(v * 100) / 100.0 + u1;
        } else if (v < 1_000_000) {
            double x = v / 1_000;
            return Math.round(x * 100) / 100.0 + u2;
        } else if (v < 1_000_000_000) {
            double x = v / 1_000_000;
            return Math.round(x * 100) / 100.0 + u3;
        } else {
            double x = v / 1_000_000_000;
            return Math.round(x * 100) / 100.0 + u4;
        }
    }

}
