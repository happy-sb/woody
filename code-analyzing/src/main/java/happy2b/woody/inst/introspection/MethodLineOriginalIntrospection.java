package happy2b.woody.inst.introspection;

import happy2b.woody.common.utils.MethodUtil;
import happy2b.woody.common.utils.Pair;
import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.common.api.Config;
import happy2b.woody.common.api.LineVisitInfo;
import happy2b.woody.common.api.MethodIntrospection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MethodLineOriginalIntrospection implements MethodIntrospection {

  private static final Logger log = LoggerFactory.getLogger(MethodLineOriginalIntrospection.class);

  private static final Set<String> SIMPLE_TYPE_CODES = new HashSet<>(Arrays.asList("byte", "Byte", "int", "Integer", "long", "Long", "double", "Double",
      "float", "Float", "short", "Short", "char", "Character", "boolean", "Boolean"));

  Method method;
  Integer methodHashcode;
  int startLine = Integer.MAX_VALUE;
  int endLine;
  int[] lineFlags;
  int[] unblockLineFlags;
  int[][] visitFlags;
  int[] startLines;
  int[][] previousAllocBlocks;
  Pair<Integer, Integer>[] explicitBlockEndpoints;
  boolean refresh = false;
  boolean instApplicable;
  String sourceCode;
  int profilingEndpointNum;

  MethodIntrospection optimizedIntrospection;

  Map<Integer, Class> anonymousClassInstanceLines = new HashMap<>();

  private Map<Integer, Set<String>> contentLines = new HashMap<>();
  private Map<Integer, Set<String>> instLines = new HashMap<>();
  private Map<Integer, List<AtomicInteger>> lineVisitFlags = new HashMap<>();
  private Set<Integer> visitMethodLine = new HashSet<>();
  private Set<Integer> optionalLines = new HashSet<>();
  Pair<Integer, Integer>[] implicitBlockEndpoints;

  private boolean visitInfoHandled = false;
  Map<Integer, Set<LineVisitInfo>> visitInfos = new HashMap<>();

  private ThreadLocal<List<long[]>> resourceCostContainer = ThreadLocal.withInitial(new Supplier<List<long[]>>() {
    @Override
    public List<long[]> get() {
      return new ArrayList(profilingEndpointNum + 5);
    }
  });

  public MethodLineOriginalIntrospection(Method method, String sourceCode) {
    this.method = method;
    this.methodHashcode = method.hashCode();
    this.sourceCode = sourceCode;
  }

  public Map<Integer, Set<LineVisitInfo>> getVisitInfos() {
    return visitInfos;
  }

  public boolean isVisitInfoHandled() {
    return visitInfoHandled;
  }

  public int getStartLine() {
    return startLine;
  }

  public int getEndLine() {
    return endLine;
  }

  public void addInstLine(Integer line, Set<String> kws) {
    Set<String> ovs = instLines.get(line);
    if (ovs != null) {
      ovs.addAll(kws);
    } else {
      instLines.put(line, kws);
    }
  }

  public void visit(int line) {
    List<AtomicInteger> visits = lineVisitFlags.get(line);
    visits.get(visits.size() - 1).incrementAndGet();
  }

  public Method getMethod() {
    return method;
  }

  @Override
  public List<long[]> getResourceCostContainer() {
    return resourceCostContainer.get();
  }

  public void refreshMethodIntrospectionLine(int line) {
    if (startLine > line) {
      startLine = line;
    }
    if (endLine < line) {
      endLine = line;
    }
    lineVisitFlags.computeIfAbsent(line, integer -> new ArrayList<>()).add(new AtomicInteger(0));
  }

  public void addContentLine(Integer line, Set<String> kws, boolean visitMethod) {
    Set<String> ovs = contentLines.get(line);
    if (ovs != null) {
      ovs.addAll(kws);
    } else {
      contentLines.put(line, kws);
    }
    if (visitMethod) {
      visitMethodLine.add(line);
    }
    visit(line);
  }

  public boolean isInstApplicable() {
    if (!refresh) {
      refreshIntrospectLines();
    }
    return instApplicable;
  }

  public int[] previousAllocBlocks(int line) {
    return previousAllocBlocks[line - startLine];
  }

  public int findStartLine(int end) {
    return startLines[end - startLine];
  }

  public void refreshIntrospectLines() {
    if (refresh) {
      return;
    }

    lineFlags = new int[endLine - startLine + 1];
    for (int i = 0; i < lineFlags.length; i++) {
      lineFlags[i] = empty_line_flag_mask;
    }

    for (Integer cl : contentLines.keySet()) {
      lineFlags[cl - startLine] = ordinary_content_flag_mask;
    }
    for (Integer il : instLines.keySet()) {
      lineFlags[il - startLine] = inst_line_flag_mask | ordinary_content_flag_mask;
    }

    for (int i = 0; i < lineFlags.length; i++) {
      if (lineFlags[i] >= ordinary_content_flag_mask) {
        startLine = startLine + i;
        break;
      }
    }

    for (int i = lineFlags.length - 1; i >= 0; i--) {
      if (lineFlags[i] >= ordinary_content_flag_mask) {
        endLine = endLine - (lineFlags.length - 1 - i);
        break;
      }
    }

    visitFlags = new int[endLine - startLine + 1][];
    for (Map.Entry<Integer, List<AtomicInteger>> entry : lineVisitFlags.entrySet()) {
      int l = entry.getKey().intValue();
      if (l < startLine || l > endLine) {
        continue;
      }
      int[] flags = new int[entry.getValue().size()];
      for (int i = 0; i < entry.getValue().size(); i++) {
        flags[i] = entry.getValue().get(i).get();
      }
      visitFlags[l - startLine] = flags;
    }

    try {
      // todo process anonymous class introspection
      this.sourceCode = fixSourceCodeLines(sourceCode, contentLines);
    } catch (Exception e) {
      log.error("[databuff-profiling]: Failed to enrich line number for source code {}, content lines {}", sourceCode, contentLines, e);
    }

    if (sourceCode != null) {

      String[] split = sourceCode.split(System.lineSeparator());

      for (int i = 0; i < lineFlags.length; i++) {
        if (lineFlags[i] == empty_line_flag_mask) {
          continue;
        }
        String line = findTargetSourceLine(split, i + startLine);
        if (line == null) {
          continue;
        }
        if (line.trim().endsWith("{") || (line.contains(" if ") || line.contains(" switch "))) {
          lineFlags[i] = lineFlags[i] | block_enter_line_flag_mask;
        }
        if ((line.contains(" if ") || line.contains(" switch "))) {
          optionalLines.add(startLine + i);
        }
        if (!noMemoryAlloc(line, i + startLine) && (lineFlags[i] & inst_line_flag_mask) == 0) {
          lineFlags[i] = lineFlags[i] | alloc_content_flag_mask;
        }
      }

      unblockLineFlags = new int[lineFlags.length];
      System.arraycopy(lineFlags, 0, unblockLineFlags, 0, lineFlags.length);

      implicitBlockEndpoints = new Pair[endLine - startLine + 1];

      splitSourceToLineBlocks(split);
      processLastUnblockedAllocLines();
      processBlockEndpoints(explicitBlockEndpoints);
      processOptionalLines();

    }

    for (int lineFlag : lineFlags) {
      if ((lineFlag & inst_line_flag_mask) > 0 || (lineFlag & block_enter_flag_mask) > 0) {
        profilingEndpointNum++;
      }
    }

    processStartLines();

    int cls = 0, ils = 0;
    for (int lineFlag : lineFlags) {
      if ((lineFlag & alloc_content_flag_mask) > 0) {
        cls++;
      } else if ((lineFlag & inst_line_flag_mask) > 0) {
        ils++;
      }
    }

    processPreviousAllocBlocks();

    instApplicable = cls > 1 && ils >= 1;

    refresh = true;
    contentLines = null;
    instLines = null;
    visitMethodLine = null;
    lineVisitFlags = null;
    optionalLines = null;
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

  public void handleVisitInfos(Object target) {
    if (visitInfos.isEmpty()) {
      return;
    }
    for (Set<LineVisitInfo> value : visitInfos.values()) {
      for (LineVisitInfo visitInfo : value) {
        try {
          handleVisitInfo(visitInfo, target);
        } catch (ClassNotFoundException e) {
          log.error("databuff-profiling: Enrich visit info occur exception!", e);
        }
      }
    }
    this.visitInfoHandled = true;
  }

  private void handleVisitInfo(LineVisitInfo visitInfo, Object target) throws ClassNotFoundException {
    Class targetClass;
    if (visitInfo.getField() == null) {
      targetClass = method.getDeclaringClass().getClassLoader().loadClass(visitInfo.getOwner().replace("/", "."));
    } else {
      Object fieldValue = ReflectionUtils.getField(visitInfo.getField(), target);
      if (fieldValue == null) {
        log.error("databuff-profiling: Can`t find field {} from class {}", visitInfo.getField().getName(), target.getClass().getName());
        return;
      }
      targetClass = fieldValue.getClass();
    }
    List<Method> methods = ReflectionUtils.findMethodIgnoreParamTypes(targetClass, visitInfo.getName());
    Optional<Method> optional = methods.stream().filter(method -> MethodUtil.getMethodDescriptor(method).equals(visitInfo.getDesc())).findAny();
    if (optional.isPresent()) {
      visitInfo.setTargetMethod(optional.get());
    }
  }

  public void processProfilingIgnores(Set<Pair<Integer, Integer>> ignores) {
    doOptimizeIntrospect(() -> optimizedIntrospection.processProfilingIgnores(ignores));
  }

  @Override
  public void processDeepIntrospect(String cpuLine, String allocLine) {
    doOptimizeIntrospect(() -> optimizedIntrospection.processDeepIntrospect(cpuLine, allocLine));
  }

  @Override
  public Integer getMethodHashcode() {
    return methodHashcode;
  }

  private void doOptimizeIntrospect(Runnable action) {
    if (optimizedIntrospection == null) {
      optimizedIntrospection = new MethodLineOptimizedIntrospection(this);
    }
    action.run();
    Config.get().refreshMethodIntrospection(method, optimizedIntrospection);
    explicitBlockEndpoints = null;
    implicitBlockEndpoints = null;
  }

  private int[] initPreviousAllocBlocks(int line) {
    int[] x = new int[2];
    boolean enter = false;
    for (int i = line - startLine - 1; i >= 0; i--) {
      if ((lineFlags[i] & inst_line_flag_mask) > 0
          || (lineFlags[i] & block_exit_flag_mask) > 0) {
        break;
      }
      if (x[0] > 0 && (lineFlags[i] & block_enter_line_flag_mask) > 0) {
        break;
      }
      if ((lineFlags[i] & alloc_content_flag_mask) > 0) {
        if (!enter) {
          x[1] = i + startLine;
        }
        x[0] = i + startLine;
        enter = true;
        continue;
      }
    }
    return x;
  }

  public String getSourceCode() {
    return sourceCode;
  }

  private String findTargetSourceLine(String[] lines, int line) {
    for (String s : lines) {
      if (s.startsWith("#" + line + ":")) {
        return s;
      }
    }
    return null;
  }

  private boolean noMemoryAlloc(String line, int lineNumber) {
    String code = line.substring(line.indexOf(":") + 1).trim().split(" ")[0];
    if ((SIMPLE_TYPE_CODES.contains(code) && !line.contains("[") && !line.contains("."))) {
      return true;
    }
    if ("String".equals(code)) {
      return !line.contains("new");
    }
    if (line.contains("=") && line.contains(" new ")) {
      return false;
    }
    if (line.contains(" catch (") || line.contains(" switch (") || line.contains(" case ")) {
      return true;
    }
    return !visitMethodLine.contains(lineNumber);
  }

  private String fixSourceCodeLines(String source, Map<Integer, Set<String>> contentLines) {
    String[] lines = source.split(System.lineSeparator());
    List<String> markedLines = new ArrayList<>();

    List<Map.Entry<Integer, Set<String>>> entries = contentLines.entrySet().stream().sorted(new Comparator<Map.Entry<Integer, Set<String>>>() {
      @Override
      public int compare(Map.Entry<Integer, Set<String>> o1, Map.Entry<Integer, Set<String>> o2) {
        return o2.getKey() - o1.getKey();
      }
    }).collect(Collectors.toList());

    for (int i = lines.length - 1; i >= 0; i--) {
      String line = lines[i];
      String ls = line.trim();
      if (ls.startsWith("#") || ls.startsWith("@") || ls.startsWith("//") || skipLine(ls)) {
        markedLines.add(line);
        continue;
      }
      if (entries.get(entries.size() - 1) == null) {
        markedLines.add(line);
        continue;
      }
      Integer ln = findMatchLineNumber(line, i == lines.length - 1 ? null : lines[i + 1], entries);
      if (ln != null) {
        markedLines.add(markLineNumber(line, ln));
      } else {
        log.warn("databuff-profiling: Failed parsed method:{} line:{}", method.getDeclaringClass().getName() + "." + method.getName(), line);
        markedLines.add(line);
      }
    }

    StringBuilder sb = new StringBuilder();
    for (int i = markedLines.size() - 1; i >= 0; i--) {
      sb.append(markedLines.get(i)).append(System.lineSeparator());
    }
    return sb.toString();
  }

  private String markLineNumber(String line, int ln) {
    String lnMark = "#" + ln + ":";
    return new StringBuilder(line).replace(0, lnMark.length(), lnMark).toString();
  }

  private Integer findMatchLineNumber(String line, String lastLine, List<Map.Entry<Integer, Set<String>>> entries) {

    boolean forLine = line.trim().startsWith("for ");
    boolean exitLine = line.contains(" return ");

    for (int i = 0; i < entries.size(); i++) {
      Map.Entry<Integer, Set<String>> entry = entries.get(i);
      if (entry == null) {
        continue;
      }
      Set<String> kws = entry.getValue();
      if (kws.isEmpty()) {
        entries.set(i, null);

        int x = i + 1;
        if (x < entries.size()) {
          Map.Entry<Integer, Set<String>> nextKws = entries.get(x);
          if (!nextKws.getValue().isEmpty() && lineMatchKeywords(line, nextKws.getValue())) {
            entries.set(x, null);
            return nextKws.getKey();
          }
        }

        return entry.getKey();
      }
      if (lineMatchKeywords(line, kws)) {
        entries.set(i, null);
        return entry.getKey();
      }
      if (exitLine) {
        if (MethodUtil.returnPrimitiveWrapType(method) && kws.size() >= 2 && kws.contains(".valueOf(")) {
          entries.set(i, null);
          return entry.getKey();
        }
      }
      if (forLine) {
        if (kws.size() == 3 && kws.contains(".iterator(") && kws.contains(".hasNext(")) {
          entries.set(i, null);
          return entry.getKey();
        }
      }
      if (lastLine != null) {
        if (lineMatchKeywords(lastLine, kws)) {
          entries.set(i, null);
          continue;
        }
      }
      return null;
    }
    return null;
  }

  private boolean lineMatchKeywords(String line, Set<String> kws) {
    boolean match = true;
    for (String kw : kws) {
      if (!line.contains(kw)) {
        match = false;
        break;
      }
    }
    return match;
  }

  private boolean skipLine(String line) {
    if (line.length() < 4) {
      return true;
    }
    if (line.startsWith("case ")) {
      return true;
    }
    String fmt = line.replaceAll("\\b(if|else|try|finally|while|default|break)\\b|[{};:()]", "");
    return fmt.length() < 3;
  }

  private Integer extractLineNumber(String line) {
    if (!line.startsWith("#")) {
      return -1;
    }
    return Integer.valueOf(line.substring(1, line.indexOf(":")).trim());
  }

  private void splitSourceToLineBlocks(String[] split) {
    explicitBlockEndpoints = new Pair[endLine - startLine + 1];
    Integer enterLN = null;
    Integer lastLN = null;
    boolean enter = false;
    boolean enterBlock = false;
    for (int i = 0; i < split.length; i++) {
      String line = split[i].trim();
      if (line.startsWith("@") || line.startsWith("//")) {
        continue;
      }
      if (line.endsWith(" {") && !enter) {
        enter = true;
        continue;
      }
      if (!enterBlock && !line.endsWith("{")) {
        continue;
      }
      if ((line.startsWith("}") || line.endsWith("}")) && enterLN != null && enterBlock) {
        explicitBlockEndpoints[enterLN - startLine] = Pair.of(enterLN, lastLN);
        enterLN = null;
        enterBlock = false;
      }
      if (line.startsWith("#")) {
        lastLN = extractLineNumber(line);
      }
      if (line.endsWith("{")) {
        enterBlock = true;
        enterLN = null;
        continue;
      }
      if (enterBlock && enterLN == null && line.startsWith("#")) {
        enterLN = extractLineNumber(line);
      }
    }
  }

  private void processLastUnblockedAllocLines() {
    int sl = 0, el = 0;
    for (int i = lineFlags.length - 1; i >= 0; i--) {
      if ((lineFlags[i] & inst_line_flag_mask) > 0 || (lineFlags[i] & block_exit_flag_mask) > 0) {
        break;
      }
      if ((lineFlags[i] & alloc_content_flag_mask) > 0) {
        sl = i;
        if (el == 0) {
          el = i;
        }
      }
    }
    if (sl > 0) {
      implicitBlockEndpoints[sl] = Pair.of(sl + startLine, el + startLine);
      lineFlags[sl] = lineFlags[sl] | block_enter_flag_mask;
      lineFlags[el] = lineFlags[el] | block_exit_flag_mask;
    }
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
      // 包含alloc和insn
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
          lineFlags[index] = lineFlags[index] | block_enter_flag_mask;
        }
        if (enterBlock && ((lineFlags[index] & inst_line_flag_mask) > 0 || i == e)) {
          enterBlock = false;
          lineFlags[lastAllocLine] = lineFlags[lastAllocLine] | block_exit_flag_mask;
        }
      }
    }
  }

  private void processOptionalLines() {
    boolean notEmpty = false;
    for (Integer opt : optionalLines) {
      Pair<Integer, Integer> optionalBlock = processOptionalLine(opt);
      if (optionalBlock != null) {
        implicitBlockEndpoints[optionalBlock.getLeft() - startLine] = optionalBlock;
        notEmpty = true;
      }
    }
    if (notEmpty) {
      processBlockEndpoints(implicitBlockEndpoints);
    }
  }

  private Pair<Integer, Integer> processOptionalLine(int opt) {
    int start = startLine, end = opt;
    for (int i = opt - startLine - 1; i >= 0; i--) {
      int lineFlag = lineFlags[i];
      if (lineFlag == empty_line_flag_mask) {
        continue;
      }
      if ((lineFlag & inst_line_flag_mask) > 0) {
        if (end == opt) {
          return null;
        } else {
          start = i + startLine + 1;
        }
        break;
      }
      if ((lineFlag & block_exit_flag_mask) > 0) {
        if (end == opt) {
          return null;
        } else {
          start = i + startLine + 1;
        }
        break;
      }
      if ((lineFlag & alloc_content_flag_mask) > 0) {
        if (end == opt) {
          end = i + startLine;
        }
      }
    }
    for (int i = start - startLine; i < end; i++) {
      if (lineFlags[i] > empty_line_flag_mask) {
        start = i + startLine;
        break;
      }
    }
    return Pair.of(start, end);
  }

  @Override
  public void addAnonymousClass(Integer line, String fullClassName) {
    Class clazz;
    if (fullClassName.contains("/")) {
      fullClassName = fullClassName.replace("/", ".");
    }
    try {
      clazz = method.getDeclaringClass().getClassLoader().loadClass(fullClassName);
    } catch (ClassNotFoundException e) {
      log.error("databuff-introspection: Cat`t find class {}", fullClassName);
      return;
    }
    this.anonymousClassInstanceLines.put(line, clazz);
  }

  @Override
  public Collection<Class> getAnonymousClass() {
    return anonymousClassInstanceLines.values();
  }

  public int getProfilingEndpointNum() {
    return profilingEndpointNum;
  }

  public int[][] getVisitFlags() {
    return visitFlags;
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

}
