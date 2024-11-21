package com.prohor.personal.personalSshWsServer;

import jakarta.websocket.DeploymentException;
import org.glassfish.tyrus.server.Server;

public class AutoCloseableServer implements AutoCloseable {
    private final Server server;

    public AutoCloseableServer(String hostName, int port, String contextPath, Class<?>... configuration) {
        server = new Server(hostName, port, contextPath, null, configuration);
    }

    public void start() throws DeploymentException {
        server.start();
    }

    @Override
    public void close() {
        server.stop();
    }
}
