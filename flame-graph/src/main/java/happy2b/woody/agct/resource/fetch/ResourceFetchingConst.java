package happy2b.woody.agct.resource.fetch;

import happy2b.woody.util.reflection.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

public interface ResourceFetchingConst {

    // ---------------------------------------------------------- Inst ---------------------------------------------------------
    String FETCHER_ADVICE_CLASS = ResourceMethodFetchTool.class.getName().replace(".", "/");

    Method SPRING_WEB_FETCHER_METHOD = ReflectionUtils.findMethod(ResourceMethodFetchTool.class, "fetchSpringWebResources", Map.class);
    String SPRING_WEB_INSTRUMENTATION_METHOD = "handlerMethodsInitialized";
    String SPRING_WEB_INSTRUMENTATION_CLASS = "org.springframework.web.servlet.handler.AbstractHandlerMethodMapping".replace(".", "/");

    Method DUBBO_FETCHER_METHOD = ReflectionUtils.findMethod(ResourceMethodFetchTool.class, "fetchDubboResources", Class.class, Object.class);
    String DUBBO_INSTRUMENTATION_METHOD = "doExport";
    String DUBBO_INSTRUMENTATION_CLASS = "org.apache.dubbo.config.ServiceConfig".replace(".", "/");

    Method GRPC_FETCHER_METHOD = ReflectionUtils.findMethod(ResourceMethodFetchTool.class, "fetchGrpcResources", Object.class);
    String GRPC_INSTRUMENTATION_METHOD_1 = "intercept";
    String GRPC_INSTRUMENTATION_METHOD_2 = "interceptForward";
    String GRPC_INSTRUMENTATION_CLASS = "io.grpc.ServerInterceptors".replace(".", "/");

    Method ROCKETMQ_FETCHER_METHOD_1 = ReflectionUtils.findMethod(ResourceMethodFetchTool.class, "fetchRocketMQResources_1", Object.class, Object.class);
    String ROCKETMQ_INSTRUMENTATION_METHOD_1_1 = "setRocketMQListener";
    String ROCKETMQ_INSTRUMENTATION_METHOD_1_2 = "setRocketMQReplyListener";
    String ROCKETMQ_INSTRUMENTATION_CLASS_1 = "org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer".replace(".", "/");

    Method ROCKETMQ_FETCHER_METHOD_2 = ReflectionUtils.findMethod(ResourceMethodFetchTool.class, "fetchRocketMQResources_2", Object.class, Object.class);
    String ROCKETMQ_INSTRUMENTATION_METHOD_2_1 = "setMessageListener";
    String ROCKETMQ_INSTRUMENTATION_METHOD_2_2 = "registerMessageListener";
    String ROCKETMQ_INSTRUMENTATION_CLASS_2 = "org.apache.rocketmq.client.consumer.DefaultMQPushConsumer".replace(".", "/");

    Method KAFKA_FETCHER_METHOD = ReflectionUtils.findMethod(ResourceMethodFetchTool.class, "fetchKafkaResource", Annotation.class, Method.class);
    String KAFKA_INSTRUMENTATION_METHOD = "processKafkaListener";
    String KAFKA_INSTRUMENTATION_CLASS = "org.springframework.kafka.annotation.KafkaListenerAnnotationBeanPostProcessor".replace(".", "/");
    // ---------------------------------------------------------- Inst ---------------------------------------------------------


    // ---------------------------------------------------------- JNI ---------------------------------------------------------
    String SPRING_WEB_FRAMEWORK_CLASS = "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping";
    String DUBBO_FRAMEWORK_CLASS = "org.apache.dubbo.config.spring.ServiceBean";
    String GRPC_FRAMEWORK_CLASS = "io.grpc.internal.ServerImpl";
    String ROCKETMQ_FRAMEWORK_CLASS_1 = "org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer";
    String ROCKETMQ_FRAMEWORK_CLASS_2 = "org.apache.rocketmq.client.consumer.DefaultMQPushConsumer";
    String KAFKA_FRAMEWORK_CLASS = "org.springframework.kafka.config.MethodKafkaListenerEndpoint";
    // ---------------------------------------------------------- JNI ---------------------------------------------------------
}
