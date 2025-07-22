package happy_sb.profiling.agct.tool;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.function.Supplier;

public class ProfilingEnvChecker {

  private static boolean isOpenJ9 = false;
  private static boolean perfEnable = true;

  private static Boolean debugSymbolsAllocApplicable;
  private static int allocEventEmptyCounter = 0;

  static {
    String vmName = System.getProperty("java.vm.name");
    if (vmName != null && vmName.contains("OpenJ9")) {
      isOpenJ9 = true;
    }

    try {
      String eventPath = "/proc/sys/kernel/perf_event_paranoid";
      BufferedReader reader = new BufferedReader(new FileReader(eventPath));
      String value = reader.readLine().trim();
      reader.close();
      if (Integer.valueOf(value) >= 2) {
        perfEnable = false;
      }

      String restrict = "/proc/sys/kernel/kptr_restrict";
      BufferedReader br = new BufferedReader(new FileReader(restrict));
      String line = br.readLine().trim();
      br.close();
      if (!line.equals("0")) {
        perfEnable = false;
      }
    } catch (Exception e) {
      perfEnable = false;
    }
  }

  private static boolean isOracleJdk() {
    String property = System.getProperty("java.vm.name");
    return property != null && property.toLowerCase().contains("hotspot");
  }

  private static boolean isOpenJdk() {
    String property = System.getProperty("java.vm.name");
    return property != null && property.toLowerCase().contains("openjdk");
  }

  private static boolean isVersionGreater(String versionA, String versionB) {
    int[] aComponents = parseVersion(versionA);
    int[] bComponents = parseVersion(versionB);

    for (int i = 0; i < Math.min(aComponents.length, bComponents.length); i++) {
      if (aComponents[i] > bComponents[i]) {
        return true;
      } else if (aComponents[i] < bComponents[i]) {
        return false;
      }
    }

    return aComponents.length > bComponents.length;
  }

  private static boolean isVersionMatch(String versionA, String versionB) {
    int[] aComponents = parseVersion(versionA);
    int[] bComponents = parseVersion(versionB);
    for (int i = 0; i < Math.min(aComponents.length, bComponents.length); i++) {
      if (aComponents[i] != bComponents[i]) {
        return false;
      }
    }
    return true;
  }

  private static int[] parseVersion(String version) {
    version = version.replaceAll("-.*", "");

    return Arrays.stream(version.split("[._-]+"))
        .filter(s -> s.matches("\\d+"))
        .mapToInt(Integer::parseInt)
        .toArray();
  }

  public static boolean isPerfEnable() {
    return perfEnable;
  }

  public static boolean isOpenJ9() {
    return isOpenJ9;
  }

  public static void handlerAllocSampleSwitcher(Supplier<Boolean> supplier) {
    if (debugSymbolsAllocApplicable != null) {
      return;
    }
    if (supplier.get()) {
      debugSymbolsAllocApplicable = true;
      return;
    }
    allocEventEmptyCounter++;
    if (debugSymbolsAllocApplicable == null && allocEventEmptyCounter >= 2) {
      debugSymbolsAllocApplicable = false;
    }
  }

  public static boolean isAllocEnable() {
    return debugSymbolsAllocApplicable == null || debugSymbolsAllocApplicable == true;
  }
}
