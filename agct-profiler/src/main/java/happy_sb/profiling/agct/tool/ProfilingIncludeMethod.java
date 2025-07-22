package happy_sb.profiling.agct.tool;



import happy_sb.profiler.util.MethodUtil;

import java.lang.reflect.Method;

public class ProfilingIncludeMethod {
    private Class clazz;
    private String methodName;
    private String signature;
    /**
     * http, dubbo, task等
     */
    private String resourceType;

    public ProfilingIncludeMethod(String resourceType, Method method) {
        this.resourceType = resourceType;
        this.methodName = method.getName();
        this.clazz = method.getDeclaringClass();
        this.signature = MethodUtil.getMethodDescriptor(method);
    }

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

}
