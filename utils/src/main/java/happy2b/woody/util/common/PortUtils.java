package happy2b.woody.util.common;

import java.io.IOException;
import java.net.ServerSocket;

public class PortUtils {

    public static synchronized int getAvailablePort(int port) {
        while (true) {
            if (isPortAvailable(port)) {
                return port;
            }
            port++;
        }
    }

    public static boolean isPortAvailable(int port) {
        try {
            ServerSocket server = new ServerSocket(port);
            server.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
