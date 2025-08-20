package happy2b.woody.agent;


import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;

/**
 * 代理启动类
 *
 * @author vlinux on 15/5/19.
 */
public class AgentBootstrap {
    private static final String WOODY_CORE_JAR = "woody-core.jar";
    private static final String WOODY_BOOTSTRAP = "happy2b.woody.core.server.WoodyBootstrap";
    private static final String GET_INSTANCE = "getInstance";
    private static final String IS_BIND = "isBind";
    private static final String BIND = "bind";

    private static PrintStream ps = System.err;

    static {
        try {
            File woodyLogDir = new File(System.getProperty("user.home") + File.separator + "logs" + File.separator
                    + "woody" + File.separator);
            if (!woodyLogDir.exists()) {
                woodyLogDir.mkdirs();
            }
            if (!woodyLogDir.exists()) {
                // #572
                woodyLogDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "logs" + File.separator
                        + "woody" + File.separator);
                if (!woodyLogDir.exists()) {
                    woodyLogDir.mkdirs();
                }
            }

            File log = new File(woodyLogDir, "woody.log");

            if (!log.exists()) {
                log.createNewFile();
            }
            ps = new PrintStream(new FileOutputStream(log, true));
        } catch (Throwable t) {
            t.printStackTrace(ps);
        }
    }

    /**
     * <pre>
     * 1. 全局持有classloader用于隔离 WOODY 实现，防止多次attach重复初始化
     * 2. ClassLoader在WOODY停止时会被reset
     * 3. 如果ClassLoader一直没变，则 com.taobao.WOODY.core.server.WOODYBootstrap#getInstance 返回结果一直是一样的
     * </pre>
     */
    private static volatile ClassLoader woodyClassLoader;

    public static void premain(String args, Instrumentation inst) {
        main(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        main(args, inst);
    }

    /**
     * 让下次再次启动时有机会重新加载
     */
    public static void resetWoodyClassLoader() {
        woodyClassLoader = null;
    }

    private static ClassLoader getClassLoader(Instrumentation inst, File woodyCoreJarFile) throws Throwable {
        // 构造自定义的类加载器，尽量减少WOODY对现有工程的侵蚀
        return loadOrDefineClassLoader(woodyCoreJarFile);
    }

    private static ClassLoader loadOrDefineClassLoader(File woodyCoreJarFile) throws Throwable {
        if (woodyClassLoader == null) {
            woodyClassLoader = new WoodyClassloader(new URL[]{woodyCoreJarFile.toURI().toURL()});
        }
        return woodyClassLoader;
    }

    private static synchronized void main(String args, final Instrumentation inst) {
        // 尝试判断woody是否已在运行，如果是的话，直接就退出
        try {
//            Class.forName("java.woody.SpyAPI");
//            if (SpyAPI.isInited()) {
//                ps.println("woody server already stared, skip attach.");
//                ps.flush();
//                return;
//            }
        } catch (Throwable e) {
            // ignore
        }
        try {
            ps.println("woody server agent start...");
            // 传递的args参数分两个部分:woodyCoreJar路径和agentArgs, 分别是Agent的JAR包路径和期望传递到服务端的参数
            if (args == null) {
                args = "";
            }
            args = decodeArg(args);

            String woodyCoreJar;
            final String agentArgs;
            int index = args.indexOf(';');
            if (index != -1) {
                woodyCoreJar = args.substring(0, index);
                agentArgs = args.substring(index + 1);
            } else {
                woodyCoreJar = "";
                agentArgs = args;
            }
            File woodyCoreJarFile = new File(woodyCoreJar);
            if (!woodyCoreJarFile.exists()) {
                ps.println("Can not find woody-core jar file from args: " + woodyCoreJarFile);
                // try to find from woody-agent.jar directory
                CodeSource codeSource = AgentBootstrap.class.getProtectionDomain().getCodeSource();
                if (codeSource != null) {
                    try {
                        File woodyAgentJarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                        woodyCoreJarFile = new File(woodyAgentJarFile.getParentFile(), WOODY_CORE_JAR);
                        if (!woodyCoreJarFile.exists()) {
                            ps.println("Can not find woody-core jar file from agent jar directory: " + woodyAgentJarFile);
                        }
                    } catch (Throwable e) {
                        ps.println("Can not find woody-core jar file from " + codeSource.getLocation());
                        e.printStackTrace(ps);
                    }
                }
            }
            if (!woodyCoreJarFile.exists()) {
                return;
            }

            /**
             * Use a dedicated thread to run the binding logic to prevent possible memory leak. #195
             */
            final ClassLoader agentLoader = getClassLoader(inst, woodyCoreJarFile);

            Thread bindingThread = new Thread() {
                @Override
                public void run() {
                    try {
                        bind(inst, agentLoader, agentArgs);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace(ps);
                    }
                }
            };

            bindingThread.setName("woody-binding-thread");
            bindingThread.start();
            bindingThread.join();
        } catch (Throwable t) {
            t.printStackTrace(ps);
            try {
                if (ps != System.err) {
                    ps.close();
                }
            } catch (Throwable tt) {
                // ignore
            }
            throw new RuntimeException(t);
        }
    }

    private static void bind(Instrumentation inst, ClassLoader agentLoader, String args) throws Throwable {
        /**
         * <pre>
         * woodyBootstrap bootstrap = woodyBootstrap.getInstance(inst);
         * </pre>
         */
        Class<?> bootstrapClass = agentLoader.loadClass(WOODY_BOOTSTRAP);
        Object bootstrap = bootstrapClass.getMethod(GET_INSTANCE, Instrumentation.class, String.class).invoke(null, inst, args);
        boolean isBind = (Boolean) bootstrapClass.getDeclaredMethod(IS_BIND).invoke(bootstrap);
        if (!isBind) {
            try {
                ps.println("Woody start to bind...");
                bootstrapClass.getDeclaredMethod(BIND).invoke(bootstrap);
                ps.println("Woody server bind success.");
                return;
            } catch (Exception e) {
                ps.println("Woody server port binding failed! Please check $HOME/logs/woody/woody.log for more details.");
                throw e;
            }
        }
        ps.println("woody server already bind.");
    }

    private static String decodeArg(String arg) {
        try {
            return URLDecoder.decode(arg, "utf-8");
        } catch (UnsupportedEncodingException e) {
            return arg;
        }
    }
}
