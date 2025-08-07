package happy2b.profiling.agct.resource.fetch;

import happy2b.profiling.agct.resource.ResourceMethod;
import happy2b.profiling.agct.resource.ResourceMethodManager;
import happy2b.profiling.agct.resource.transform.ResourceClassManager;

public interface IResourceFetcher {

    void bootstrap();

    static void addResourceMethod(ResourceMethod method) {
        ResourceMethodManager.addProfilingIncludeMethod(method);
        Class<?> clazz = method.getMethod().getDeclaringClass();
        if (clazz.getName().startsWith("org.springframework")) {
            return;
        }
        ResourceClassManager.INSTANCE.addResourceClass(clazz);
    }

}
