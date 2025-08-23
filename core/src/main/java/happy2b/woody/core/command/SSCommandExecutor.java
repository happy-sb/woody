package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.core.flame.common.constant.ProfilingResourceType;
import happy2b.woody.core.flame.resource.ResourceMethod;
import happy2b.woody.core.flame.core.ResourceFetcherManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0
 * @description: select resource -t(type) kafka [order1,order2,..]
 * @since 2025/8/22
 */
public class SSCommandExecutor implements WoodyCommandExecutor {

    public static final String COMMAND_NAME = "ss";

    @Override
    public String commandName() {
        return COMMAND_NAME;
    }

    @Override
    public boolean support(WoodyCommand command) {
        return command.getEval().startsWith("ss ");
    }

    public void executeInternal(WoodyCommand command) {
        String[] segments = command.getEval().split(" ");

        String orderSegment = null;
        String type = null;
        boolean clear = false;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.equals(commandName())) {
                continue;
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
            } else if (orderSegment == null) {
                orderSegment = segment;
            } else if (segment.equals("-c")) {
                clear = true;
            } else {
                command.error("invalid command segment: " + segment);
                return;
            }
        }

        if (clear && type != null) {
            command.error("clear selected resources not support '-t' argument!");
            return;
        }

        if (clear) {
            ResourceFetcherManager.INSTANCE.clearSelectedResources();
            command.result("clear selected resource success!");
            return;
        }

        List<Integer> orders = null;
        if (orderSegment != null) {
            orders = new ArrayList<>();
            Set<ResourceMethod> methods = ResourceFetcherManager.INSTANCE.listResources(type);
            Set<Integer> allOrders = methods.stream().map(ResourceMethod::getOrder).collect(Collectors.toSet());
            for (String order : orderSegment.split(",")) {
                int i = Integer.parseInt(order);
                if (!allOrders.contains(i)) {
                    command.error("invalid order:" + order);
                    return;
                }
                orders.add(i);
            }
        }
        if (orders == null) {
            ResourceFetcherManager.INSTANCE.selectResources(type);
            command.result(String.format("添加 %s 成功", type));
        } else {
            ResourceFetcherManager.INSTANCE.selectResources(type, orders);
            command.result(String.format("添加 %s:%s 成功", type, orders));
        }
    }
}
