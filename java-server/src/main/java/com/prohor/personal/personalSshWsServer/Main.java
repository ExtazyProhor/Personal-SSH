package com.prohor.personal.personalSshWsServer;

import org.glassfish.tyrus.server.Server;

import static com.prohor.personal.personalSshWsServer.Variables.getVariables;

public class Main {
    public static void main(String[] args) throws Exception {
        Server server = null;
        try {
            server = new Server("localhost", getVariables().getInt("ws-server-port"), "/", null, WebSocketServer.class);
            server.start();
            System.out.println(System.in.read());
        } finally {
            if (server != null)
                server.stop();
        }
    }
}
