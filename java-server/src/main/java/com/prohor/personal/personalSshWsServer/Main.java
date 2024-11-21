package com.prohor.personal.personalSshWsServer;

import static com.prohor.personal.personalSshWsServer.Variables.getVariables;

public class Main {
    public static void main(String[] args) throws Exception {
        try (AutoCloseableServer server = new AutoCloseableServer(
                "localhost", getVariables().getInt("ws-server-port"), "/", null, WebSocketServer.class)) {
            server.start();
            System.out.println(System.in.read());
        }
    }
}
