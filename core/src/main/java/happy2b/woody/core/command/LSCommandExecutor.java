package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.core.flame.common.constant.ProfilingResourceType;
import happy2b.woody.core.flame.resource.ResourceMethod;
import happy2b.woody.core.flame.resource.fetch.ResourceFetcherManager;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0
 * @description: list resource -s(selected) -t(type)
 * @since 2025/8/22
 */
public class LSCommandExecutor implements WoodyCommandExecutor {

    @Override
    public String commandName() {
        return "ls";
    }

    @Override
    public boolean support(WoodyCommand command) {
        return command.getEval().startsWith(commandName());
    }

    @Override
    public void execute(WoodyCommand command) {
        String[] segments = command.getEval().split(" ");

        String type = null;
        boolean selected = false;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (segment.equals(commandName())) {
                continue;
            } else if (segment.equals("-s")) {
                selected = true;
            } else if (segment.equals("-t")) {
                type = segments[i + 1];
                if (ProfilingResourceType.ofType(type) == null) {
                    command.error("invalid resource type: " + type);
                    return;
                }
                if (i == segments.length - 1) {
                    command.error("miss '-t' parameter value.");
                    return;
                }
                i++;
            } else {
                command.error("invalid command: " + command);
                return;
            }
        }

        StringBuilder sb = new StringBuilder();
        if (selected) {
            if (type != null) {
                Set<ResourceMethod> methods = ResourceFetcherManager.listSelectedResources(type);
                appendResourceFormatString(sb, type, methods);
            } else {
                Map<String, Set<ResourceMethod>> methods = ResourceFetcherManager.listAllSelectedResources();
                for (Map.Entry<String, Set<ResourceMethod>> entry : methods.entrySet()) {
                    appendResourceFormatString(sb, entry.getKey(), entry.getValue());
                }
            }
        } else {
            if (type != null) {
                Set<ResourceMethod> methods = ResourceFetcherManager.listResources(type);
                appendResourceFormatString(sb, type, methods);
            } else {
                Map<String, Set<ResourceMethod>> methods = ResourceFetcherManager.listAllResources();
                for (Map.Entry<String, Set<ResourceMethod>> entry : methods.entrySet()) {
                    appendResourceFormatString(sb, entry.getKey(), entry.getValue());
                }
            }
        }

        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        command.result(sb.toString().trim());
    }

    private void appendResourceFormatString(StringBuilder sb, String type, Set<ResourceMethod> methods) {
        if (sb.length() == 0) {
            sb.append("\n");
        }
        List<ResourceMethod> sortedMethods = methods.stream().sorted(Comparator.comparingInt(ResourceMethod::getOrder)).collect(Collectors.toList());
        sb.append(type).append(":\n");
        for (ResourceMethod method : sortedMethods) {
            sb.append("  ").append(method.getOrder()).append(". ").append(method.getResource()).append("\n");
        }
    }

}
