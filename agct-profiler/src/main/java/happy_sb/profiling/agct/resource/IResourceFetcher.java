package happy_sb.profiling.agct.resource;

public interface IResourceFetcher {

    void bootstrap();

    void transformTracingMethod(ResourceMethod method);

}
