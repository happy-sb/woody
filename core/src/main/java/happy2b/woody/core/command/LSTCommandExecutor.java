package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.core.flame.core.ResourceFetcherManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0
 * @description: list resource types
 * @since 2025/8/23
 */
public class LSTCommandExecutor implements WoodyCommandExecutor {

    public static final String COMMAND_NAME = "lst";

    @Override
    public String commandName() {
        return COMMAND_NAME;
    }

    @Override
    public boolean support(WoodyCommand command) {
        return command.getEval().equals(commandName());
    }

    @Override
    public void executeInternal(WoodyCommand command) {
        List<String> types = ResourceFetcherManager.INSTANCE.listAllAvailableResourceTypes();
        String collect = types.stream().collect(Collectors.joining(","));
        command.result("[" + collect + "]");
    }
}
