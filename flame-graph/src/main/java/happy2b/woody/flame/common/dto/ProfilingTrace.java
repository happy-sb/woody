package happy2b.woody.flame.common.dto;

import java.util.ArrayList;
import java.util.List;

public class ProfilingTrace {

    private String resource;
    private String type;
    private String methodPath;
    private Object traceId;
    private long startTime;
    private long endTime;
    private List<ProfilingSpan> spanList = new ArrayList<>();

    public static final ProfilingTrace NO_OP = new ProfilingTrace("NO_OP", null, null, 0) {
        @Override
        public void finish() {

        }
    };

    public ProfilingTrace(String resource, String type, String methodPath, Object traceId) {
        this.resource = resource;
        this.type = type;
        this.methodPath = methodPath;
        this.traceId = traceId;
        this.startTime = System.nanoTime();
    }

    public ProfilingSpan startSpan(Object spanId, long startTime, String operationName) {
        if (endTime > 0) {
            return null;
        }
        ProfilingSpan span = new ProfilingSpan(this.traceId, spanId, startTime, operationName, resource);
        this.spanList.add(span);
        return span;
    }

    public void finish() {
        this.endTime = System.nanoTime();
    }

    public Object getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isFinished() {
        return endTime > 0;
    }

    public boolean contains(long timestamp) {
        return timestamp >= startTime && timestamp <= endTime;
    }

    public String getResource() {
        return resource;
    }

    public ProfilingSpan findSpanByTime(long sampleTime) {
        if (spanList.isEmpty()) {
            return new ProfilingSpan(traceId, -1, startTime, type, resource);
        }
        long startTime = spanList.get(0).getStartTime();
        long endTime = spanList.get(spanList.size() - 1).getEndTime();
        if (sampleTime < startTime || sampleTime > endTime) {
            return new ProfilingSpan(traceId, -1, startTime, type, resource);
        }

        int left = 0;
        int right = spanList.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            ProfilingSpan span = spanList.get(mid);

            if (span.contains(sampleTime)) {
                return spanList.get(mid);
            } else if (sampleTime < span.getStartTime()) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        return new ProfilingSpan(traceId, -1, startTime, type, resource);
    }
}
