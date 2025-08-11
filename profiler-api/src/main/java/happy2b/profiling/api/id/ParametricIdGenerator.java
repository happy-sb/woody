package happy2b.profiling.api.id;

public interface ParametricIdGenerator<T> extends IdGenerator {

    T generateTraceId(Object param);

    T generateSpanId(Object param);

    /**
     * 参数索引, 从0开始
     *
     * @return
     */
    int paramIndex();

    @Override
    default Object generateTraceId() {
        throw new UnsupportedOperationException("generateTraceId");
    }

    @Override
    default Object generateSpanId() {
        throw new UnsupportedOperationException("generateSpanId");
    }
}
