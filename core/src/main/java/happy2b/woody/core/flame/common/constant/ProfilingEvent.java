package happy2b.woody.core.flame.common.constant;

public enum ProfilingEvent {
    CPU("cpu"),
    ALLOC("alloc"),
    WALL("wall"),
    LOCK("lock");

    private String segment;

    ProfilingEvent(String segment) {
        this.segment = segment;
    }

    public String getSegment() {
        return segment;
    }
}
