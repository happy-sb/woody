package happy2b.profiling.agct.resource.fetch.inst;

import happy2b.profiler.util.bytecode.InstrumentationUtils;
import happy2b.profiling.agct.resource.fetch.IResourceFetcher;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/5
 */
public class InstResourceFetcher implements IResourceFetcher {

    public static final IResourceFetcher INSTANCE = new InstResourceFetcher();

    @Override
    public void bootstrap() {
        InstrumentationUtils.getInstrumentation().addTransformer(new ResourceFetcherTransformer());
    }

}
