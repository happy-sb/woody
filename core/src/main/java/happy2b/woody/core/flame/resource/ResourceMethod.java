package happy2b.woody.core.flame.resource;


import happy2b.woody.common.utils.MethodUtil;
import happy2b.woody.common.api.id.IdGenerator;

import java.lang.reflect.Method;

public class ResourceMethod {
    private int order;
    private Class clazz;
    private String methodName;
    private String descriptor;

    private Method method;

    private String methodPath;
    /**
     * http, dubbo, task等
     */
    private String resourceType;
    private String resource;

    private IdGenerator idGenerator;

    public ResourceMethod(String resourceType, String resource, Method method) {
        this(resourceType, resource, method, IdGenerator.INSTANCE);
    }

    public ResourceMethod(String resourceType, String resource, Method method, IdGenerator idGenerator) {
        this.resourceType = resourceType;
        this.resource = resource;
        this.methodName = method.getName();
        this.method = method;
        this.methodPath = method.getDeclaringClass().getName() + "." + method.getName();
        this.clazz = method.getDeclaringClass();
        this.descriptor = MethodUtil.getMethodDescriptor(method);
        this.idGenerator = idGenerator;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Class getClazz() {
        return clazz;
    }


    public String getMethodName() {
        return methodName;
    }


    public String getSignature() {
        return descriptor;
    }


    public String getResourceType() {
        return resourceType;
    }


    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public String getResource() {
        return resource;
    }

    public Method getMethod() {
        return method;
    }

    public String getMethodPath() {
        return methodPath;
    }
}
