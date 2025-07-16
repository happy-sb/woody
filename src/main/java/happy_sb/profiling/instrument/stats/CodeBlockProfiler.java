package happy_sb.profiling.instrument.stats;

import java.util.concurrent.atomic.LongAdder;

import static happy_sb.profiling.instrument.stats.MethodProfilingManager.formatProfiling;


public class CodeBlockProfiler {
    int startLine;
    int endLine;
    String formatLine;
    LongAdder times;
    LongAdder cpuUsages;
    LongAdder memoryUsages;
    LongAdder timeUsages;

    public CodeBlockProfiler(int startLine, int endLine) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.formatLine = startLine == endLine ? startLine + "" : startLine + "-" + endLine;
        this.times = new LongAdder();
        this.cpuUsages = new LongAdder();
        this.memoryUsages = new LongAdder();
        this.timeUsages = new LongAdder();
    }

    public CodeBlockProfiler(String formatLine) {
        this.formatLine = formatLine;
        this.times = new LongAdder();
        this.cpuUsages = new LongAdder();
        this.memoryUsages = new LongAdder();
        this.timeUsages = new LongAdder();
    }

    public void accumulate(CodeBlockProfiler profiling) {
        this.times.add(profiling.times.sum());
        this.cpuUsages.add(profiling.cpuUsages.sum());
        this.memoryUsages.add(profiling.memoryUsages.sum());
        this.timeUsages.add(profiling.timeUsages.sum());
    }

    public String formatStatsInfo() {
        return formatProfiling(formatLine, times.sum(), cpuUsages.sum(), memoryUsages.sum(), timeUsages.sum());
    }

    public void refresh() {
        times.reset();
        cpuUsages.reset();
        memoryUsages.reset();
        timeUsages.reset();
    }
}
