package happy2b.woody.boot;

import happy2b.woody.common.utils.AnsiLog;
import happy2b.woody.common.utils.PortUtils;

import java.io.File;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/15
 */
public class Bootstrap {

    private static final int SERVER_BINDING_PORT = 9050;

    public static void main(String[] args) throws Exception {
        long pid = 0;
        // select pid
        try {
            pid = ProcessUtils.select(false);
        } catch (InputMismatchException e) {
            System.out.println("Please input an integer to select pid.");
            System.exit(1);
        }
        if (pid < 0) {
            System.out.println("Please select an available pid.");
            System.exit(1);
        }
        File woodyHomeDir = null;
        CodeSource codeSource = Bootstrap.class.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            try {
                File bootJarPath = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                verifyWoodyHome(bootJarPath.getParent());
                woodyHomeDir = bootJarPath.getParentFile();
            } catch (Throwable e) {
                throw e;
            }

        }

        int availablePort = PortUtils.getAvailablePort(SERVER_BINDING_PORT);

        List<String> attachArgs = new ArrayList<>();
        attachArgs.add("-jar");
        attachArgs.add(new File(woodyHomeDir, "woody-core.jar").getAbsolutePath());
        attachArgs.add("-pid");
        attachArgs.add("" + pid);
        attachArgs.add("-core");
        attachArgs.add(new File(woodyHomeDir, "woody-core.jar").getAbsolutePath());
        attachArgs.add("-agent");
        attachArgs.add(new File(woodyHomeDir, "woody-agent.jar").getAbsolutePath());
        attachArgs.add("-serverPort");
        attachArgs.add(availablePort + "");
        AnsiLog.info("Try to attach process " + pid);
        AnsiLog.debug("Start woody-core.jar args: " + attachArgs);
        ProcessUtils.startArthasCore(pid, attachArgs);

        AnsiLog.info("Attach process {} success.", pid);

        // 启动woody-client
        AnsiLog.info("woody-client connect {} {}", "127.0.0.1", availablePort);

        System.setProperty("jline.terminal", "org.jline.terminal.impl.PosixTerminal");
        System.setProperty("jline.terminal.dumb", "false");

        new WoodyClient("127.0.0.1", availablePort, pid).boot();
    }

    private static void verifyWoodyHome(String arthasHome) {
        File home = new File(arthasHome);
        if (home.isDirectory()) {
            String[] fileList = {"woody-core.jar", "woody-agent.jar"};

            for (String fileName : fileList) {
                if (!new File(home, fileName).exists()) {
                    throw new IllegalArgumentException(fileName + " do not exist, arthas home: " + home.getAbsolutePath());
                }
            }
            return;
        }

        throw new IllegalArgumentException("illegal arthas home: " + home.getAbsolutePath());
    }
}
