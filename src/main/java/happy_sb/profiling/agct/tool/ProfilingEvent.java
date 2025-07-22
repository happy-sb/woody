package happy_sb.profiling.agct.tool;

public enum ProfilingEvent {
    CPU("cpu"),
    ALLOC("alloc"),
    WALL("wall"),
    LOCK("lock");

    private String segment;

    ProfilingEvent(String segment) {
        this.segment = segment;
    }
}
