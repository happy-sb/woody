package happy2b.woody.agct.resource.fetch;

import happy2b.woody.agct.constant.ProfilingResourceType;
import happy2b.woody.agct.resource.ResourceMethod;
import happy2b.woody.agct.resource.ResourceMethodManager;
import happy2b.woody.agct.resource.transform.ResourceClassManager;

public interface IResourceFetcher {

    void bootstrap(ProfilingResourceType... types);

    static void addResourceMethod(ResourceMethod method) {
        ResourceMethodManager.addProfilingIncludeMethod(method);
        Class<?> clazz = method.getMethod().getDeclaringClass();
        if (clazz.getName().startsWith("org.springframework")) {
            return;
        }
        ResourceClassManager.INSTANCE.addResourceClass(clazz);
    }

}
