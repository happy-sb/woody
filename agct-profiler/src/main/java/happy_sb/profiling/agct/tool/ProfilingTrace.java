package happy_sb.profiling.agct.tool;

import java.util.ArrayList;
import java.util.List;

public class ProfilingTrace {

  private String resource;
  private String type;
  private String traceId;
  private long startTime;
  private long endTime;
  private List<ProfilingSpan> spanList = new ArrayList<>();

  public ProfilingTrace(String resource, String type, String traceId, long startTime) {
    this.resource = resource;
    this.type = type;
    this.traceId = traceId;
    this.startTime = startTime;
  }

  public ProfilingSpan startSpan(long spanId, long startTime, String operationName) {
    if (endTime > 0) {
      return null;
    }
    ProfilingSpan span = new ProfilingSpan(this.traceId, spanId, startTime, operationName, resource);
    this.spanList.add(span);
    return span;
  }

  public void finish(long endTime) {
    this.endTime = endTime;
  }

  public String getTraceId() {
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
    if(spanList.isEmpty()){
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
