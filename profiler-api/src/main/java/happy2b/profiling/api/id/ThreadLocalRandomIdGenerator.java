package happy2b.profiling.api.id;

import java.util.concurrent.ThreadLocalRandom;

public class ThreadLocalRandomIdGenerator implements IdGenerator<Long> {

    public static final ThreadLocalRandomIdGenerator INSTANCE = new ThreadLocalRandomIdGenerator();

    @Override
    public Long generateTraceId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }

    @Override
    public Long generateSpanId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
