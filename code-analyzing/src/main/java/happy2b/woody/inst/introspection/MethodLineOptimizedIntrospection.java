package happy2b.woody.inst.introspection;

import happy2b.woody.util.Pair;
import happy2b.woody.api.Config;
import happy2b.woody.api.LineVisitInfo;
import happy2b.woody.api.MethodIntrospection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;

public class MethodLineOptimizedIntrospection implements MethodIntrospection {

    private static final Logger log = LoggerFactory.getLogger(MethodLineOptimizedIntrospection.class);

    private Method method;
    private Integer methodHashcode;
    private String sourceCode;
    private int startLine;
    private int endLine;

    private int[] lineFlags;
    private int[][] visitFlags;
    private int[] startLines;
    private int[][] previousAllocBlocks;

    private boolean multiDeepIntrospectLine;
    private Pair<Integer, Integer>[] explicitBlockEndpoints;
    private Pair<Integer, Integer>[] implicitBlockEndpoints;

    private int profilingEndpointNum;

    private Map<Integer, Set<LineVisitInfo>> visitInfos;

    private ThreadLocal<List<long[]>> resourceCostContainer = ThreadLocal.withInitial(new Supplier<List<long[]>>() {
        @Override
        public List<long[]> get() {
            return new ArrayList(profilingEndpointNum);
        }
    });

    public MethodLineOptimizedIntrospection(MethodLineOriginalIntrospection introspection) {
        this.method = introspection.method;
        this.methodHashcode = method.hashCode();
        this.sourceCode = introspection.sourceCode;
        this.startLine = introspection.startLine;
        this.endLine = introspection.endLine;
        this.lineFlags = introspection.unblockLineFlags;
        this.visitFlags = introspection.visitFlags;
        this.startLines = introspection.startLines;
        this.visitInfos = introspection.visitInfos;
        this.implicitBlockEndpoints = introspection.implicitBlockEndpoints;
        this.explicitBlockEndpoints = introspection.explicitBlockEndpoints;
    }

    @Override
    public int getStartLine() {
        return startLine;
    }

    @Override
    public int getEndLine() {
        return endLine;
    }

    @Override
    public String getSourceCode() {
        return sourceCode;
    }

    @Override
    public boolean isVisitInfoHandled() {
        return true;
    }

    public boolean isOnCodeBlockEnter(int line) {
        return (lineFlags[line - startLine] & block_enter_flag_mask) > 0;
    }

    public boolean isOnCodeBlockExit(int line) {
        return (lineFlags[line - startLine] & block_exit_flag_mask) > 0;
    }

    public boolean isInsnLine(int line) {
        return (lineFlags[line - startLine] & inst_line_flag_mask) > 0;
    }

    @Override
    public int[][] getVisitFlags() {
        return visitFlags;
    }

    @Override
    public int findStartLine(int end) {
        return startLines[end - startLine];
    }

    @Override
    public int getProfilingEndpointNum() {
        return profilingEndpointNum;
    }

    @Override
    public int[] previousAllocBlocks(int line) {
        return previousAllocBlocks[line - startLine];
    }

    @Override
    public Map<Integer, Set<LineVisitInfo>> getVisitInfos() {
        return visitInfos;
    }

    @Override
    public void processProfilingIgnores(Set<Pair<Integer, Integer>> ignores) {
        for (Pair<Integer, Integer> ignore : ignores) {
            int si = ignore.getLeft() - startLine;
            int ei = ignore.getRight() - startLine;
            for (int i = si; i <= ei; i++) {
                if (lineFlags[i] > ordinary_content_flag_mask) {
                    lineFlags[i] = ignored_flag_mask;
                }
            }
        }
        processBlockEndpoints(explicitBlockEndpoints);

        if (implicitBlockEndpoints != null) {
            expandImplicitBlocks();
            processBlockEndpoints(implicitBlockEndpoints);
        }

        processLastUnblockedAllocLines();

        processStartLines();

        processPreviousAllocBlocks();

    }

    public void processDeepIntrospect(String cpuLine, String allocLine) {
        if (cpuLine == null && allocLine == null) {
            this.visitInfos = null;
            return;
        }
        if (cpuLine != null) {
            deepIntrospectLine(cpuLine);
        }
        if (allocLine != null) {
            deepIntrospectLine(allocLine);
        }
//    this.visitInfos = null;
    }

    @Override
    public Integer getMethodHashcode() {
        return methodHashcode;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public List<long[]> getResourceCostContainer() {
        return resourceCostContainer.get();
    }

    private void processBlockEndpoints(Pair<Integer, Integer>[] lineEndpoints) {
        for (Pair<Integer, Integer> block : lineEndpoints) {
            if (block == null) {
                continue;
            }
            Integer s = block.getLeft();
            Integer e = block.getRight();

            boolean hasInst = false;
            boolean hasAlloc = false;
            for (int i = s; i <= e; i++) {
                if ((lineFlags[i - startLine] & alloc_content_flag_mask) > 0) {
                    hasAlloc = true;
                }
                if ((lineFlags[i - startLine] & inst_line_flag_mask) > 0) {
                    hasInst = true;
                }
            }
            if (!hasAlloc) {
                continue;
            }
            if (!hasInst) {
                lineFlags[s - startLine] = lineFlags[s - startLine] | block_enter_flag_mask;
                lineFlags[e - startLine] = lineFlags[e - startLine] | block_exit_flag_mask;
                continue;
            }
            boolean enterBlock = false;
            int lastAllocLine = 0;
            for (int i = s; i <= e; i++) {
                int index = i - startLine;
                if (!enterBlock && (lineFlags[index] & inst_line_flag_mask) > 0) {
                    continue;
                }

                if ((lineFlags[index] & alloc_content_flag_mask) > 0) {
                    enterBlock = true;
                    lastAllocLine = index;
                    int sl = index;
                    for (int j = i - startLine - 1; j >= s - startLine; j--) {
                        if ((lineFlags[j] & inst_line_flag_mask) > 0) {
                            break;
                        }
                        if (lineFlags[j] == ignored_flag_mask) {
                            sl = j;
                        }
                    }
                    lineFlags[sl] = lineFlags[sl] | block_enter_flag_mask;
                }

                if (enterBlock && ((lineFlags[index] & inst_line_flag_mask) > 0 || i == e)) {
                    enterBlock = false;
                    int el = lastAllocLine;
                    for (int j = i - startLine + 1; j <= e - startLine; j++) {
                        if ((lineFlags[j] & inst_line_flag_mask) > 0) {
                            break;
                        }
                        if (lineFlags[j] == ignored_flag_mask) {
                            el = j;
                        }
                    }
                    lineFlags[el] = lineFlags[el] | block_exit_flag_mask;
                }
            }
        }
    }

    private void expandImplicitBlocks() {

        for (int i = 0; i < implicitBlockEndpoints.length; i++) {
            Pair<Integer, Integer> optionalBlock = implicitBlockEndpoints[i];
            if (optionalBlock == null) {
                continue;
            }
            int st = optionalBlock.getLeft();
            int et = optionalBlock.getRight();
            for (int j = st - startLine - 1; j >= 0; j--) {
                if (lineFlags[j] < ignored_flag_mask) {
                    continue;
                }
                if (lineFlags[j] > ignored_flag_mask || inExplicitBlock(j + startLine)) {
                    break;
                } else {
                    implicitBlockEndpoints[i] = Pair.of(j + startLine, et);
                }
            }
            for (int j = et - startLine + 1; j < lineFlags.length; j++) {
                if (lineFlags[j] < ignored_flag_mask) {
                    continue;
                }
                if (lineFlags[j] > ignored_flag_mask || inExplicitBlock(j + startLine)) {
                    break;
                } else {
                    implicitBlockEndpoints[i] = Pair.of(implicitBlockEndpoints[i].getLeft(), j + startLine);
                }
            }
        }
    }

    private void processLastUnblockedAllocLines() {
        int sl = 0, el = 0;
        int ignore = -1;
        for (int i = lineFlags.length - 1; i >= 0; i--) {
            if ((lineFlags[i] & inst_line_flag_mask) > 0 || inExplicitBlock(i + startLine)) {
                break;
            }
            if (lineFlags[i] == ignored_flag_mask) {
                ignore = i;
                if (sl > 0) {
                    sl = Math.min(sl, ignore);
                }
                continue;
            }
            if ((lineFlags[i] & alloc_content_flag_mask) > 0) {
                sl = Math.min(i, ignore);
                if (el == 0) {
                    el = Math.max(i, ignore);
                }
            }
        }
        if (sl > 0) {
            lineFlags[sl] = lineFlags[sl] | block_enter_flag_mask;
            lineFlags[el] = lineFlags[el] | block_exit_flag_mask;
        }
    }

    private boolean inExplicitBlock(int line) {
        for (Pair<Integer, Integer> endpoint : explicitBlockEndpoints) {
            if (endpoint == null) {
                continue;
            }
            if (endpoint.getLeft() <= line && line <= endpoint.getRight()) {
                return true;
            }
        }
        return false;
    }

    private void processStartLines() {
        startLines = new int[endLine - startLine + 1];
        for (int i = endLine - startLine; i >= 0; i--) {
            if ((lineFlags[i] & inst_line_flag_mask) > 0) {
                startLines[i] = startLine + i;
                continue;
            }
            if ((lineFlags[i] & block_exit_flag_mask) > 0) {
                for (int j = i; j >= 0; j--) {
                    if ((lineFlags[j] & block_enter_flag_mask) > 0 || (lineFlags[j] & inst_line_flag_mask) > 0) {
                        startLines[i] = startLine + j;
                        break;
                    }
                }
            }
        }
    }


    private void deepIntrospectLine(String line) {
        Pair<Integer, Integer> pair = extractLineEndpoint(line);

        List<LineVisitInfo> candidates = new ArrayList<>();
        for (int i = pair.getLeft(); i <= pair.getRight(); i++) {
            Set<LineVisitInfo> visitInfos = this.visitInfos.get(i);
            if (visitInfos != null) {
                candidates.addAll(visitInfos);
            }
        }

        if (candidates.isEmpty()) {
            return;
        }
        if (candidates.size() > 1) {
            log.info("databuff-profiling: doDeepIntrospectLine line visit info size > 1 !");
            multiDeepIntrospectLine = true;
        } else {
            Method targetMethod = candidates.get(0).getTargetMethod();
            log.info("databuff-profiling: Deep introspect method {}#{} for root method {}#{}", targetMethod.getDeclaringClass().getName(), targetMethod.getName(), method.getDeclaringClass().getName(), method.getName());
            Config.get().getPendingTransformMethods().computeIfAbsent(method, method -> new HashSet<>()).add(targetMethod);
        }

    }

    private void processPreviousAllocBlocks() {
        previousAllocBlocks = new int[endLine - startLine + 1][];
        for (int i = 0; i <= endLine - startLine; i++) {
            if ((lineFlags[i] & inst_line_flag_mask) > 0 || (lineFlags[i] & block_enter_flag_mask) > 0) {
                int[] ints = initPreviousAllocBlocks(i + startLine);
                if (ints != null && ints[0] > 0) {
                    previousAllocBlocks[i] = ints;
                }
            }
        }
    }

    private int[] initPreviousAllocBlocks(int line) {
        int[] x = new int[2];
        boolean enter = false;
        int lastIgnoreLine = 0;
        for (int i = line - startLine - 1; i >= 0; i--) {
            if ((lineFlags[i] & inst_line_flag_mask) > 0
                    || (lineFlags[i] & block_exit_flag_mask) > 0) {
                break;
            }
            if (x[0] > 0 && (lineFlags[i] & block_enter_line_flag_mask) > 0) {
                break;
            }
            if ((lineFlags[i] & ignored_flag_mask) > 0) {
                if (enter) {
                    x[0] = i + startLine;
                } else {
                    lastIgnoreLine = i;
                }
                continue;
            }
            if ((lineFlags[i] & alloc_content_flag_mask) > 0) {
                if (!enter) {
                    x[1] = Math.max(lastIgnoreLine, i) + startLine;
                }
                x[0] = i + startLine;
                enter = true;
                continue;
            }
        }
        return x;
    }

    private Pair<Integer, Integer> extractLineEndpoint(String line) {
        if (line.contains("-")) {
            String[] split = line.split("-");
            return Pair.of(Integer.valueOf(split[0]), Integer.valueOf(split[1]));
        } else {
            return Pair.of(Integer.valueOf(line), Integer.valueOf(line));
        }
    }
}
