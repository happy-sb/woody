package happy2b.woody.flame.resource.fetch.inst;

import happy2b.woody.util.bytecode.InstrumentationUtils;
import happy2b.woody.flame.common.constant.ProfilingResourceType;
import happy2b.woody.flame.resource.fetch.IResourceFetcher;

import java.util.*;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/5
 */
public class InstResourceFetcher implements IResourceFetcher {

    public static final IResourceFetcher INSTANCE = new InstResourceFetcher();

    @Override
    public void bootstrap(ProfilingResourceType... types) {
        Set<String> resourceTypes = new HashSet<>();
        for (ProfilingResourceType type : types) {
            resourceTypes.addAll(Arrays.asList(type.instResourceClasses()));
        }
        InstrumentationUtils.getInstrumentation().addTransformer(new ResourceFetcherTransformer(resourceTypes));
    }

}
