package happy2b.woody.common.api;

import static java.util.concurrent.TimeUnit.*;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/26
 */
public class NanoTimer {

    private final long startTimeNano;
    private final long startNanoTicks;

    private volatile long lastSyncTicks;
    private final long clockSyncPeriod;

    private volatile long counterDrift;

    public static final NanoTimer INSTANCE = new NanoTimer();


    private NanoTimer() {
        this.startTimeNano = MILLISECONDS.toNanos(System.currentTimeMillis());
        this.startNanoTicks = System.nanoTime();
        this.lastSyncTicks = startNanoTicks;
        this.clockSyncPeriod = Math.max(1_000_000L, SECONDS.toNanos(30));
    }

    public long getNanoTime() {
        return getTimeWithNanoTicks(System.nanoTime());
    }

    private long getTimeWithNanoTicks(long nanoTicks) {
        long computedNanoTime = startTimeNano + Math.max(0, nanoTicks - startNanoTicks);
        if (nanoTicks - lastSyncTicks >= clockSyncPeriod) {
            long drift = computedNanoTime - getCurrentTimeNanos();
            if (Math.abs(drift + counterDrift) >= 1_000_000L) { // allow up to 1ms of drift
                counterDrift = -MILLISECONDS.toNanos(NANOSECONDS.toMillis(drift));
            }
            lastSyncTicks = nanoTicks;
        }
        return computedNanoTime + counterDrift;
    }

    private long getCurrentTimeNanos() {
        return MILLISECONDS.toNanos(System.currentTimeMillis());
    }
}
