package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/21
 */
public class CommandExecutor {

    private static final List<WoodyCommandExecutor> WOODY_COMMANDS = new ArrayList<>();

    static {
        WOODY_COMMANDS.add(new StopCommandExecutor());
        WOODY_COMMANDS.add(new LSCommandExecutor());
    }

    public static WoodyCommand execute(String command) {
        WoodyCommand cmd = new WoodyCommand(System.currentTimeMillis(), command);
        for (WoodyCommandExecutor woodyCommand : WOODY_COMMANDS) {
            if (woodyCommand.support(cmd)) {
                woodyCommand.execute(cmd);
                return cmd;
            }
        }
        cmd.error("不支持命令:" + command);
        return cmd;
    }

}
