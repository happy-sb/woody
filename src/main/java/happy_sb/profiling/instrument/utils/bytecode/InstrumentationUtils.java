package happy_sb.profiling.instrument.utils.bytecode;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class InstrumentationUtils {
    private static final Logger logger = LoggerFactory.getLogger(InstrumentationUtils.class);

    private InstrumentationUtils() {
    }

    private static final AtomicReference<Instrumentation> instrumentationRef =
            new AtomicReference<>();

    public static void setInstrumentation(Instrumentation instrumentation) {
        instrumentationRef.compareAndSet(null, instrumentation);
    }

    public static Instrumentation getInstrumentation() {
        return instrumentationRef.get();
    }

    public static boolean retransformClasses(ClassFileTransformer transformer, Class... classes) {
        Instrumentation inst = instrumentationRef.get();
        try {
            inst.addTransformer(transformer,true);
            inst.retransformClasses(classes);
            return true;
        } catch (UnmodifiableClassException e) {
            logger.error("[databuff-profiling-span]: RetransformClasses class error, classes:{}", classes, e);
            inst.removeTransformer(transformer);
            return false;
        }
    }

    public static void retransformClasses(ClassFileTransformer transformer, List<Class<?>> classes) {
        Instrumentation inst = instrumentationRef.get();
        try {
            inst.addTransformer(transformer, true);
            for (Class<?> clazz : classes) {
                if (isLambdaClass(clazz)) {
                    continue;
                }
                try {
                    inst.retransformClasses(clazz);
                } catch (Throwable e) {
                    logger.error("[databuff-profiling-span]: RetransformClasses class error, class:{}", clazz.getName(), e);
                }
            }
        } finally {
            inst.removeTransformer(transformer);
        }
    }

    public static boolean isLambdaClass(Class<?> clazz) {
        return clazz.getName().contains("$$Lambda$");
    }
}
