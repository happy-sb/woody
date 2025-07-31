package happy_sb.profiling.inst.stats;

import happy_sb.profiler.util.Pair;
import happy_sb.profiling.api.Config;
import happy_sb.profiling.api.MethodIntrospection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static happy_sb.profiling.inst.stats.MethodProfilingManager.*;


public class MethodProfiler {

    private static final Logger log = LoggerFactory.getLogger(MethodProfiler.class);

    private static final int MAXIMUM_STATS_DURATION_IN_MINUTE = 3;

    private static final int MINUTE_BASED_PROFILER_NUM = 15;

    private static final Map<Method, Set<Pair<Integer, Integer>>> IGNORE_PROFILING_LINES = new ConcurrentHashMap<>();

    private Method method;
    private int startLine;
    private int endLine;
    private String sourceCode;
    private final CodeBlockProfiler[][] blockProfiling;
    private int[] invokeTimes;
    private int startIndex;

    MethodProfiler(Method method, int index) {
        this.method = method;
        MethodIntrospection introspection = Config.get().getMethodLineIntrospection(method);
        this.sourceCode = introspection.getSourceCode();
        this.startLine = introspection.getStartLine();
        this.endLine = introspection.getEndLine();
        this.blockProfiling = new CodeBlockProfiler[MINUTE_BASED_PROFILER_NUM][];
        for (int i = 0; i < blockProfiling.length; i++) {
            blockProfiling[i] = new CodeBlockProfiler[endLine - startLine + 1];
        }
        this.invokeTimes = new int[MINUTE_BASED_PROFILER_NUM];
        this.startIndex = index;
    }

    void compilationProfiling(int index, List<long[]> profiling) {
        invokeTimes[index % MINUTE_BASED_PROFILER_NUM]++;

        for (long[] longs : profiling) {
            int i = index % MINUTE_BASED_PROFILER_NUM;
            int j = (int) (longs[0] - startLine);
            if (i < 0 || j < 0) {
                continue;
            }
            CodeBlockProfiler stats = blockProfiling[i][j];
            if (stats == null) {
                blockProfiling[i][j] = new CodeBlockProfiler((int) longs[0], (int) longs[1]);
                stats = blockProfiling[i][j];
            }
            stats.times.add(1);
            stats.cpuUsages.add(longs[2]);
            stats.timeUsages.add(longs[3]);
            stats.memoryUsages.add(longs[4]);
        }
    }

    String getProfilingStats(int index, int duration) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s#%s", method.getDeclaringClass().getName(), method.getName())).append(System.lineSeparator());
        sb.append(sourceCode).append(System.lineSeparator());
        sb.append(statsLatestMinutesProfiling(index, duration));
        return sb.toString();
    }

    synchronized void refresh(int index) {
        for (CodeBlockProfiler profiling : blockProfiling[(index + 1) % MINUTE_BASED_PROFILER_NUM]) {
            if (profiling != null) {
                profiling.refresh();
            }
        }
        invokeTimes[(index + 1) % MINUTE_BASED_PROFILER_NUM] = 0;
    }

    private synchronized String statsLatestMinutesProfiling(int index, int duration) {
        int invokeTime = 0;
        CodeBlockProfiler[][] values = new CodeBlockProfiler[duration][];
        int x = 0;
        for (int i = (index) % MINUTE_BASED_PROFILER_NUM; i >= 0 && x < duration; i--) {
            values[x++] = blockProfiling[i];
            invokeTime += invokeTimes[i];
        }
        if (x < duration) {
            for (int i = blockProfiling.length - 1; i >= 0 && x < duration; i--) {
                values[x++] = blockProfiling[i];
                invokeTime += invokeTimes[i];
            }
        }

        if (invokeTime == 0) {
            return null;
        }

        Map<String, CodeBlockProfiler> profilingMap = new HashMap<>();
        for (CodeBlockProfiler[] profilings : values) {
            for (CodeBlockProfiler profiling : profilings) {
                if (profiling == null || profiling.times.sum() == 0) {
                    continue;
                }
                profilingMap.computeIfAbsent(profiling.formatLine, s -> new CodeBlockProfiler(s)).accumulate(profiling);
            }
        }
        List<String> sortedLines = profilingMap.keySet().stream().sorted(Comparator.comparingInt(this::extractStartLine)).collect(Collectors.toList());

        long totalCpu = 0, totalAlloc = 0, totalDuration = 0;
        for (Map.Entry<String, CodeBlockProfiler> entry : profilingMap.entrySet()) {
            totalCpu += entry.getValue().cpuUsages.sum();
            totalAlloc += entry.getValue().memoryUsages.sum();
            totalDuration += entry.getValue().timeUsages.sum();
        }

        if (index - startIndex >= MAXIMUM_STATS_DURATION_IN_MINUTE && !IGNORE_PROFILING_LINES.containsKey(method)) {
            processProfilingIgnoresAndDeepIntrospect(profilingMap, invokeTime, totalCpu, totalAlloc);
        }

        StringBuilder sb = new StringBuilder(stats_title);
        sb.append(System.lineSeparator());

        String totalFormat = formatString(
                "|", segment_length, duration + "Min", invokeTime + "",
                formatTime(totalCpu), formatTime(totalCpu / invokeTime),
                formatAlloc(totalAlloc), formatAlloc(totalAlloc / invokeTime),
                formatTime(totalDuration), formatTime(totalDuration / invokeTime));
        sb.append(totalFormat).append(System.lineSeparator());

        for (String line : sortedLines) {
            sb.append(profilingMap.get(line).formatStatsInfo()).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private int extractStartLine(String formatLine) {
        return !formatLine.contains("-") ? Integer.parseInt(formatLine) : Integer.parseInt(formatLine.substring(0, formatLine.indexOf("-")));
    }

    private Pair<Integer, Integer> toLineBlock(String formatLine) {
        if (!formatLine.contains("-")) {
            return Pair.of(Integer.parseInt(formatLine), Integer.parseInt(formatLine));
        }
        String[] split = formatLine.split("-", 2);
        return Pair.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }

    private void processProfilingIgnoresAndDeepIntrospect(Map<String, CodeBlockProfiler> profilingMap, int invokeTime, long totalCpu, long totalAlloc) {
        Set<Pair<Integer, Integer>> ignoreLines = new HashSet<>();
        String cpuIntrospectLine = null, allocIntrospectLine = null;
        for (Map.Entry<String, CodeBlockProfiler> entry : profilingMap.entrySet()) {
            CodeBlockProfiler value = entry.getValue();
            if (value.cpuUsages.sum() / invokeTime < profiling_cpu_threshold
                    && value.memoryUsages.sum() / invokeTime < profiling_alloc_threshold) {
                log.info("Ignore instrumentation profiling method {}#{}, line {}", method.getDeclaringClass().getName(), method.getName(), value.formatLine);
                ignoreLines.add(toLineBlock(value.formatLine));
            }
            if (value.cpuUsages.sum() * 100 / totalCpu > deep_introspect_resource_cost_percent_threshold) {
                cpuIntrospectLine = value.formatLine;
            }
            if (value.memoryUsages.sum() * 100 / totalAlloc > deep_introspect_resource_cost_percent_threshold) {
                allocIntrospectLine = value.formatLine;
            }
        }

        Config.get().getMethodLineIntrospection(method).processProfilingIgnores(ignoreLines);
        if (!ignoreLines.isEmpty()) {
            PENDING_RETRANSFORM_METHODS.add(method);
        }

        Config.get().getMethodLineIntrospection(method).processDeepIntrospect(cpuIntrospectLine, allocIntrospectLine);

        IGNORE_PROFILING_LINES.put(method, ignoreLines.isEmpty() ? Collections.EMPTY_SET : ignoreLines);
    }
}
