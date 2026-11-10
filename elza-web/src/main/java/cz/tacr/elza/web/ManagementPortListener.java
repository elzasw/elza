package cz.tacr.elza.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Resolves the port of the management (Actuator) server before the application
 * context starts.
 *
 * When {@code management.server.port} is configured, it is used as is; the
 * application does not start when it equals {@code server.port} or another
 * process already listens on it. When it is not configured, the first free port
 * from {@code server.port + 1} up to {@code server.port + MAX_PORT_OFFSET} is
 * used and logged, so the default installation (8080) keeps the management
 * port 8081. The value {@code -1} switches the management server off.
 */
public class ManagementPortListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ManagementPortListener.class);

    static final String SERVER_PORT = "server.port";
    static final String MANAGEMENT_PORT = "management.server.port";
    static final String MANAGEMENT_ADDRESS = "management.server.address";
    static final String PROPERTY_SOURCE_NAME = "elzaManagementPort";

    static final int DEFAULT_SERVER_PORT = 8080;
    static final int MAX_PORT_OFFSET = 10;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        resolve(event.getEnvironment());
    }

    static void resolve(ConfigurableEnvironment env) {
        int serverPort = env.getProperty(SERVER_PORT, Integer.class, DEFAULT_SERVER_PORT);
        String address = env.getProperty(MANAGEMENT_ADDRESS);
        Integer configuredPort = env.getProperty(MANAGEMENT_PORT, Integer.class);

        if (configuredPort != null) {
            checkConfiguredPort(configuredPort, serverPort, address);
            return;
        }

        int port;
        if (serverPort <= 0) {
            // random application port -> random management port
            port = 0;
        } else {
            port = findFreePort(serverPort, address);
            logger.info("Management (actuator) port is not configured ({}), using {} on {}",
                        MANAGEMENT_PORT, port, address != null ? address : "all interfaces");
        }
        env.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME,
                Map.of(MANAGEMENT_PORT, port)));
    }

    private static void checkConfiguredPort(int port, int serverPort, String address) {
        if (port <= 0) {
            // -1 = management server off, 0 = random port
            return;
        }
        if (port == serverPort) {
            throw new IllegalStateException("Management (actuator) port " + MANAGEMENT_PORT + "=" + port
                    + " equals the application port " + SERVER_PORT
                    + ". Set a different port, or -1 to switch the management endpoints off.");
        }
        if (!isFree(address, port)) {
            throw new IllegalStateException("Management (actuator) port " + MANAGEMENT_PORT + "=" + port
                    + " is already in use by another process. Set a different port, or -1 to switch"
                    + " the management endpoints off.");
        }
    }

    private static int findFreePort(int serverPort, String address) {
        int lastPort = Math.min(serverPort + MAX_PORT_OFFSET, 65535);
        for (int port = serverPort + 1; port <= lastPort; port++) {
            if (isFree(address, port)) {
                return port;
            }
            logger.info("Port {} is in use, trying the next one for the management (actuator) server", port);
        }
        throw new IllegalStateException("No free port for the management (actuator) server in the range "
                + (serverPort + 1) + "-" + lastPort + ". Set " + MANAGEMENT_PORT
                + " to a free port, or -1 to switch the management endpoints off.");
    }

    static boolean isFree(String address, int port) {
        InetSocketAddress socketAddress = address != null
                ? new InetSocketAddress(address, port)
                : new InetSocketAddress(port);
        // a default server socket uses the same SO_REUSEADDR setting as Tomcat, so a port
        // left in TIME_WAIT by a restart is reported as free
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(socketAddress);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
