package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.common.thread.AgentThreadFactory;
import happy2b.woody.core.flame.common.dto.ProfilingSample;
import happy2b.woody.core.flame.common.dto.ProfilingSampleBase;
import happy2b.woody.core.flame.core.ProfilingManager;
import happy2b.woody.core.flame.core.ResourceFetcherManager;
import happy2b.woody.core.flame.core.TraceManager;
import happy2b.woody.core.flame.resource.ResourceMethod;
import happy2b.woody.core.server.WoodyBootstrap;
import happy2b.woody.core.tool.ProfilingSampleProcessor;
import happy2b.woody.core.tool.graph.FlameGraph;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0 2025/8/23 10:05 AM
 * @description: profiling start [-d ];  profiling stop
 * @since 2025/8/23
 */
public class ProfilingCommandExecutor implements WoodyCommandExecutor {

    public static final String COMMAND_NAME = "profiling";

    private static final int DEFAULT_PROFILING_DURATION = 600;

    private volatile boolean profiling;
    private Thread profilingThread;

    @Override
    public String commandName() {
        return COMMAND_NAME;
    }

    @Override
    public boolean support(WoodyCommand command) {
        return command.getEval().startsWith(COMMAND_NAME + " ");
    }

    @Override
    public void executeInternal(WoodyCommand command) {
        String[] segments = command.getEval().split(" ");
        Operation operation = null;
        Integer duration = 0;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.equals(commandName())) {
                continue;
            } else if (segment.equals("start")) {
                operation = Operation.START;
            } else if (segment.equals("stop")) {
                operation = Operation.STOP;
            } else if (segment.equals("-d")) {
                if (operation != Operation.START) {
                    command.error("invalid command segment: " + segment);
                    return;
                }
                if (i == segments.length - 1) {
                    command.error("miss profiling duration param value!");
                    return;
                }
                duration = Integer.parseInt(segments[++i]);
            }
        }
        if (operation == null) {
            command.error("invalid profiling command!");
            return;
        }
        if (operation == Operation.START) {
            if (profiling) {
                command.error("profiling already started!");
                return;
            }
            if (duration == null) {
                duration = DEFAULT_PROFILING_DURATION;
            }
            Map<String, Long> intervals = ProfilingManager.INSTANCE.getEventIntervals();
            if (intervals.isEmpty()) {
                command.error("no event interval set!");
                return;
            }
            Map<String, Set<ResourceMethod>> resources = ResourceFetcherManager.INSTANCE.listAllSelectedResources();
            if (resources.isEmpty()) {
                command.error("no resource selected!");
                return;
            }
            startProfiling(command, duration);
        } else if (operation == Operation.STOP) {
            if (!profiling) {
                command.error("profiling not started or already stop!");
                return;
            }
            profilingThread.interrupt();
        }
    }

    private void startProfiling(WoodyCommand command, int duration) {
        try {
            ProfilingManager.INSTANCE.startProfiling();
            command.result("start profiling successful!");

            profilingThread = AgentThreadFactory.newAgentThread(AgentThreadFactory.AgentThread.PROFILING_WORKER, () -> {
                try {
                    profiling = true;
                    Thread.sleep(duration);
                } catch (InterruptedException e1) {
                } finally {
                    profiling = false;
                    profilingThread = null;
                    stopProfiling(command);
                }
            });
            profilingThread.start();
        } catch (Throwable e) {
            command.error(buildFailedMessage(command.getEval(), e));
        }
    }

    private void stopProfiling(WoodyCommand command) {
        try {
            List<ProfilingSample> profilingSamples = ProfilingManager.INSTANCE.stopProfiling();
            ProfilingSampleProcessor.populateTracingInfo(profilingSamples, TraceManager.profilingTraces);
            Map<String, ProfilingSampleBase> sampleBaseMap = ProfilingSampleProcessor.extractSampleBase(profilingSamples);
            Map<String, List<ProfilingSample>> eventSamples = profilingSamples.stream().collect(Collectors.groupingBy(ProfilingSample::getEventType));
            for (Map.Entry<String, List<ProfilingSample>> entry : eventSamples.entrySet()) {
                FlameGraph.convert(entry.getValue(), sampleBaseMap, WoodyBootstrap.getInstance().getWoodyHome() + File.separator + entry.getKey() + "_flameGraph.html");
            }
            command.result("profiling stopped!");
        } catch (Throwable t) {
            command.error(buildFailedMessage(command.getEval(), t));
        } finally {
            command.setTime(System.currentTimeMillis());
            WoodyBootstrap.getInstance().writeCommand(command);
        }
    }

    private enum Operation {
        START, STOP
    }
}
