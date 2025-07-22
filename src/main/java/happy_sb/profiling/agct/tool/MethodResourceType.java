package happy_sb.profiling.agct.tool;

public enum MethodResourceType {
    HTTP("http"),
    DUBBO("dubbo"),
    GRPC("grpc"),
    ROCKETMQ("rocketmq"),
    KAFKA("kafka"),
    TASK("task");

    private String value;

    MethodResourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
