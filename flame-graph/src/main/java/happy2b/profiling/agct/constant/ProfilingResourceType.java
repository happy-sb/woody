package happy2b.profiling.agct.constant;


import static happy2b.profiling.agct.resource.fetch.ResourceFetchingConst.*;

public enum ProfilingResourceType {

    HTTP("http") {
        @Override
        public String[] resourceClasses() {
            return new String[]{SPRING_WEB_INSTRUMENTATION_CLASS};
        }
    },
    DUBBO("dubbo") {
        @Override
        public String[] resourceClasses() {
            return new String[]{DUBBO_INSTRUMENTATION_CLASS};
        }
    },
    GRPC("grpc") {
        @Override
        public String[] resourceClasses() {
            return new String[]{GRPC_INSTRUMENTATION_CLASS};
        }
    },
    ROCKETMQ("rocketmq") {
        @Override
        public String[] resourceClasses() {
            return new String[]{ROCKETMQ_INSTRUMENTATION_CLASS_1, ROCKETMQ_INSTRUMENTATION_CLASS_2};
        }
    },
    KAFKA("kafka") {
        @Override
        public String[] resourceClasses() {
            return new String[]{KAFKA_INSTRUMENTATION_CLASS};
        }
    };

    private String value;

    ProfilingResourceType(String segment) {
        this.value = segment;
    }

    public abstract String[] resourceClasses();

}
