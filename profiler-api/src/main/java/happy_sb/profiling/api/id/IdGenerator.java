package happy_sb.profiling.api.id;

public interface IdGenerator<T> {

    IdGenerator INSTANCE = ThreadLocalRandomIdGenerator.INSTANCE;

    T generateTraceId();

    T generateSpanId();

    int getOrder();

}
