package happy2b.woody.agct.constant;


import static happy2b.woody.agct.resource.fetch.ResourceFetchingConst.*;

public enum ProfilingResourceType {

    HTTP("http") {
        @Override
        public String[] instResourceClasses() {
            return new String[]{SPRING_WEB_INSTRUMENTATION_CLASS};
        }

        @Override
        public String[] jniResourceClasses() {
            return new String[]{SPRING_WEB_FRAMEWORK_CLASS};
        }
    },
    DUBBO("dubbo") {
        @Override
        public String[] instResourceClasses() {
            return new String[]{DUBBO_INSTRUMENTATION_CLASS};
        }

        @Override
        public String[] jniResourceClasses() {
            return new String[]{DUBBO_FRAMEWORK_CLASS};
        }
    },
    GRPC("grpc") {
        @Override
        public String[] instResourceClasses() {
            return new String[]{GRPC_INSTRUMENTATION_CLASS};
        }

        @Override
        public String[] jniResourceClasses() {
            return new String[]{GRPC_FRAMEWORK_CLASS};
        }
    },
    ROCKETMQ("rocketmq") {
        @Override
        public String[] instResourceClasses() {
            return new String[]{ROCKETMQ_INSTRUMENTATION_CLASS_1, ROCKETMQ_INSTRUMENTATION_CLASS_2};
        }

        @Override
        public String[] jniResourceClasses() {
            return new String[]{ROCKETMQ_FRAMEWORK_CLASS_1, ROCKETMQ_FRAMEWORK_CLASS_2};
        }
    },
    KAFKA("kafka") {
        @Override
        public String[] instResourceClasses() {
            return new String[]{KAFKA_INSTRUMENTATION_CLASS};
        }

        @Override
        public String[] jniResourceClasses() {
            return new String[]{KAFKA_FRAMEWORK_CLASS};
        }
    };

    private String value;

    ProfilingResourceType(String segment) {
        this.value = segment;
    }

    public String getValue() {
        return value;
    }

    public abstract String[] instResourceClasses();

    public abstract String[] jniResourceClasses();

}
