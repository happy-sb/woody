package happy_sb.profiling.inst.tools;


import happy_sb.profiling.api.MethodIntrospection;

public class ProfilingEntity {
    public long cpuSnapshot;
    public long memorySnapshot;
    public long timeSnapshot;
    public long tid;
    public volatile boolean used = false;
    public MethodIntrospection introspection;
}
