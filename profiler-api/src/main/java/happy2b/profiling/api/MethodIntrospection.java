package happy2b.profiling.api;


import happy2b.profiler.util.Pair;

import java.lang.reflect.Method;
import java.util.*;

public interface MethodIntrospection {

  int empty_line_flag_mask =        0b00000000;
  int ordinary_content_flag_mask =  0b00000010;
  int ignored_flag_mask =           0b00000100;
  int alloc_content_flag_mask =     0b00001000;
  int block_enter_flag_mask =       0b00010000;
  int block_exit_flag_mask =        0b00100000;
  int inst_line_flag_mask =         0b01000000;
  int block_enter_line_flag_mask =  0b10000000;

  int getStartLine();

  int getEndLine();

  default void addContentLine(Integer line, Set<String> kws, boolean visitMethod) {

  }

  default void addInstLine(Integer line, Set<String> kws) {

  }

  default void visit(int line) {

  }

  default void refreshMethodIntrospectionLine(int line) {

  }

  boolean isVisitInfoHandled();

  boolean isOnCodeBlockEnter(int line);

  boolean isOnCodeBlockExit(int line);

  boolean isInsnLine(int line);

  default boolean isInstApplicable() {
    return true;
  }

  default void handleVisitInfos(Object target) {

  }

  default void refreshIntrospectLines() {

  }

  int[][] getVisitFlags();

  int findStartLine(int end);

  int getProfilingEndpointNum();

  int[] previousAllocBlocks(int line);

  Map<Integer, Set<LineVisitInfo>> getVisitInfos();

  String getSourceCode();

  void processProfilingIgnores(Set<Pair<Integer, Integer>> ignores);

  void processDeepIntrospect(String cpuLine, String allocLine);

  Integer getMethodHashcode();

  Method getMethod();

  List<long[]> getResourceCostContainer();

  default void addAnonymousClass(Integer line, String fullClassName){}

  default Collection<Class> getAnonymousClass(){
    return Collections.emptySet();
  }

}
