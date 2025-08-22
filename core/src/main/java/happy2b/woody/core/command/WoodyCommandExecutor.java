package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;

public interface WoodyCommandExecutor {

    String commandName();

    boolean support(WoodyCommand command);

    void execute(WoodyCommand command);

    default String buildFailedMessage(String command, Throwable t) {
        return "执行命令:'" + command + "’失败,失败信息:" + t.getMessage() + ", 相信错误 信息请查看${user.home}/.woody/woody.log文件";
    }

}
