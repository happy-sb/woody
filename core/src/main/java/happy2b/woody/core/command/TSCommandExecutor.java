package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.core.flame.common.constant.ProfilingEvent;
import happy2b.woody.core.flame.common.dto.ProfilingSample;
import happy2b.woody.core.flame.common.dto.TraceSamples;
import happy2b.woody.core.flame.core.ProfilingManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0
 * @description: trace sample
 * -l list
 * -f flame graph file
 * -c clear
 * --id traceId
 * --e event
 * --top N
 * @since 2025/8/25
 */
public class TSCommandExecutor implements WoodyCommandExecutor {

    private static final int DEFAULT_TOP_N = 10;

    @Override
    public String commandName() {
        return "ts";
    }

    @Override
    public boolean support(WoodyCommand command) {
        return command.getEval().startsWith(commandName() + " ");
    }

    @Override
    public void executeInternal(WoodyCommand command) {
        String[] segments = command.getEval().split(" ");

        int topN = 0;
        int opCount = 0;
        String file = null, id = null;
        ProfilingEvent event = null;
        boolean list = false, fg = false, clear = false;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.equals(commandName())) {

            } else if (segment.equals("-l")) {
                list = true;
                opCount++;
            } else if (segment.equals("-f")) {
                if (i == segments.length - 1) {
                    command.error("missing trace sample file param value!");
                    return;
                }
                fg = true;
                file = segments[++i];
                opCount++;
            } else if (segment.equals("-c")) {
                clear = true;
                opCount++;
            } else if (segment.equals("--id")) {
                if (i == segments.length - 1) {
                    command.error("missing trace sample id param value!");
                    return;
                }
                id = segments[++i];
            } else if (segment.equals("--top")) {
                if (i == segments.length - 1) {
                    command.error("missing trace sample topN param value!");
                    return;
                }
                topN = Integer.parseInt(segments[++i]);
            } else if (segment.equals("--e")) {
                if (i == segments.length - 1) {
                    command.error("missing trace sample event param value!");
                    return;
                }
                event = ProfilingEvent.ofValue(segments[++i]);
                if (event == null) {
                    command.error("invalid trace sample event param value: " + segments[i]);
                    return;
                }
            } else {
                command.error("invalid profiling command!");
                return;
            }
        }
        if (opCount != 1) {
            command.error("invalid trace sample command!");
            return;
        }
        if (topN > 100) {
            command.error("topN must less than 100");
            return;
        }
        if (topN > 0 && id != null) {
            command.error("topN and ids can not be set at the same time");
            return;
        }
        if (clear) {
            ProfilingManager.INSTANCE.setTraceSamples(null);
            command.result("clear trace samples success!");
            return;
        }

        if (list) {
            listTraceSample(command, topN, event, id);
            return;
        }

    }

    private void listTraceSample(WoodyCommand command, int topN, ProfilingEvent event, String id) {
        TraceSamples traceSamples = ProfilingManager.INSTANCE.getTraceSamples();
        Map<String, List<ProfilingSample>> eventSamples = traceSamples.getEventSamples();

        if (eventSamples.size() > 1 && event == null) {
            command.error("profiling event can not be null when list multi event trace samples!");
            return;
        }
        List<ProfilingSample> samples;
        if (event == null) {
            samples = eventSamples.values().iterator().next();
        } else {
            samples = eventSamples.get(event.getSegment());
        }
        if (id != null) {
            List<ProfilingSample> targetSamples = samples.stream().filter(profilingSample -> id.equals(profilingSample.getTraceId())).collect(Collectors.toList());
            if (targetSamples.isEmpty()) {
                command.result("[]");
            } else {
                formatSamples(command, targetSamples);
            }
        } else {
            topN = (topN == 0) ? DEFAULT_TOP_N : topN;

        }
    }

    private void formatSamples(WoodyCommand command, List<ProfilingSample> samples) {
        StringBuilder sb = new StringBuilder();
        samples = samples.stream().sorted((o1, o2) -> o1.getSampleTime() > o2.getSampleTime() ? -1 : 1).collect(Collectors.toList());
        for (ProfilingSample sample : samples) {

        }
    }

    private void sortProfilingSamples(List<ProfilingSample> samples) {

    }
}
