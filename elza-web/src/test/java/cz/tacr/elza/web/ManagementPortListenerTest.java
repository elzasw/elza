package cz.tacr.elza.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ManagementPortListenerTest {

    private static final String LOOPBACK = "127.0.0.1";

    private static ServerSocket bindLoopback(int port) throws IOException {
        ServerSocket socket = new ServerSocket();
        socket.bind(new InetSocketAddress(LOOPBACK, port));
        return socket;
    }

    /**
     * Finds a free loopback port whose two following ports are free as well, so the
     * tests can occupy the ports right after the application port.
     */
    private static int freeServerPort() throws IOException {
        for (int i = 0; i < 20; i++) {
            int port;
            try (ServerSocket socket = bindLoopback(0)) {
                port = socket.getLocalPort();
            }
            if (port < 65000 && ManagementPortListener.isFree(LOOPBACK, port + 1)
                    && ManagementPortListener.isFree(LOOPBACK, port + 2)) {
                return port;
            }
        }
        throw new IllegalStateException("No free port pair found");
    }

    private static MockEnvironment environment(int serverPort) {
        return new MockEnvironment()
                .withProperty(ManagementPortListener.SERVER_PORT, String.valueOf(serverPort))
                .withProperty(ManagementPortListener.MANAGEMENT_ADDRESS, LOOPBACK);
    }

    @Test
    void notConfiguredUsesNextPort() throws IOException {
        int serverPort = freeServerPort();
        MockEnvironment env = environment(serverPort);

        ManagementPortListener.resolve(env);

        assertThat(env.getProperty(ManagementPortListener.MANAGEMENT_PORT, Integer.class))
                .isEqualTo(serverPort + 1);
    }

    @Test
    void notConfiguredSkipsUsedPort() throws IOException {
        int serverPort = freeServerPort();
        MockEnvironment env = environment(serverPort);

        try (ServerSocket used = bindLoopback(serverPort + 1)) {
            ManagementPortListener.resolve(env);
        }

        assertThat(env.getProperty(ManagementPortListener.MANAGEMENT_PORT, Integer.class))
                .isEqualTo(serverPort + 2);
    }

    @Test
    void notConfiguredWithRandomServerPortUsesRandomPort() {
        MockEnvironment env = environment(0);

        ManagementPortListener.resolve(env);

        assertThat(env.getProperty(ManagementPortListener.MANAGEMENT_PORT, Integer.class)).isZero();
    }

    @Test
    void configuredSameAsServerPortFails() {
        MockEnvironment env = environment(8081)
                .withProperty(ManagementPortListener.MANAGEMENT_PORT, "8081");

        assertThatThrownBy(() -> ManagementPortListener.resolve(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("equals the application port");
    }

    @Test
    void configuredUsedPortFails() throws IOException {
        int serverPort = freeServerPort();
        MockEnvironment env = environment(serverPort)
                .withProperty(ManagementPortListener.MANAGEMENT_PORT, String.valueOf(serverPort + 1));

        try (ServerSocket used = bindLoopback(serverPort + 1)) {
            assertThatThrownBy(() -> ManagementPortListener.resolve(env))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already in use");
        }
    }

    @Test
    void configuredFreePortIsKept() throws IOException {
        int serverPort = freeServerPort();
        MockEnvironment env = environment(serverPort)
                .withProperty(ManagementPortListener.MANAGEMENT_PORT, String.valueOf(serverPort + 2));

        ManagementPortListener.resolve(env);

        assertThat(env.getProperty(ManagementPortListener.MANAGEMENT_PORT, Integer.class))
                .isEqualTo(serverPort + 2);
    }

    @Test
    void configuredDisabledIsKept() {
        MockEnvironment env = environment(8081)
                .withProperty(ManagementPortListener.MANAGEMENT_PORT, "-1");

        ManagementPortListener.resolve(env);

        assertThat(env.getProperty(ManagementPortListener.MANAGEMENT_PORT, Integer.class)).isEqualTo(-1);
    }
}
