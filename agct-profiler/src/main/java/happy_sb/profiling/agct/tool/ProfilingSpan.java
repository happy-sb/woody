package happy_sb.profiling.agct.tool;

public class ProfilingSpan {
  private String traceId;
  private long spanId;
  private long startTime;
  private String operation;
  private String rootMethod;
  private long endTime;

  public ProfilingSpan(String traceId, long spanId, long startTime, String operationName, String resource) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.startTime = startTime;
    this.operation = operationName;
    this.rootMethod = resource;
  }

  public long getSpanId() {
    return spanId;
  }

  public void setSpanId(long spanId) {
    this.spanId = spanId;
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

  public void finish(long endTime) {
    this.endTime = endTime;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public boolean contains(long timestamp) {
    return timestamp >= startTime && timestamp <= endTime;
  }

  public String getRootMethod() {
    return rootMethod;
  }
}
