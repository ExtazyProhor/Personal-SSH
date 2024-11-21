package com.prohor.personal.personalSshWsServer;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

@ServerEndpoint(value = "/")
public class WebSocketServer {
    private Session agentSession;
    private Session clientSession;
    private final Set<String> newSessions = new HashSet<>();
    private final Consumer<Throwable> log;
    private final Map<String, Command> commands = new HashMap<>();
    private final Map<Integer, ResultWaiter> waiters = new HashMap<>();
    private int lastId = 0;
    public static WebSocketServer webSocketServer;
    private CycleExecutor cycleExecutor;

    public WebSocketServer() {
        webSocketServer = this;
        String strPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (strPath.endsWith(".jar"))
            log = new FileLogger(new File(strPath).getParentFile());
        else
            log = Throwable::printStackTrace;
        try {
            updateCommands(Variables.getVariables());
        } catch (IOException e) {
            log.accept(e);
        }
    }

    private void updateCommands(JSONObject var) {
        commands.clear();
        var.getJSONArray("commands").forEach(o -> {
            if (o instanceof JSONObject json) {
                commands.put(json.getString("keyword"), new Command(
                        json.getString("command"),
                        json.getString("result")
                ));
            }
        });
    }

    @OnOpen
    public synchronized void onOpen(Session session) {
        try {
            if (newSessions.size() >= 2) {
                session.close();
                return;
            }
            newSessions.add(session.getId());
            session.getBasicRemote().sendText("ping");
        } catch (IOException e) {
            onError(session, e);
        }
    }

    @OnMessage
    public synchronized void onMessage(Session session, String message) {
        JSONObject json = new JSONObject(message);

        if (newSessions.contains(session.getId())) {
            newSessions.remove(session.getId());
            if (message.equals("rust-pong"))
                agentSession = session;
            else if (message.equals("js-pong"))
                clientSession = session;
            return;
        }
        try {
            if (isKnownSession(session, agentSession)) {
                int id = json.getInt("id");
                if (waiters.containsKey(id)) {
                    ResultWaiter waiter = waiters.get(id);
                    waiter.interrupt();
                }
            } else if (isKnownSession(session, clientSession)) {
                message = json.getString("action");
                if (message.equals("stop-cycle")) {
                    if (cycleExecutor == null) {
                        sendMessageToClient("there is no cycle", true);
                        return;
                    }
                    cycleExecutor.interrupt();
                    cycleExecutor = null;
                    sendMessageToClient("cycle was interrupted", false);
                } else if (message.equals("netsh-cycle")) {
                    if (!commands.containsKey("netsh"))
                        return;
                    if (cycleExecutor != null) {
                        sendMessageToClient("cycle has already started", true);
                        return;
                    }
                    Command command = commands.get("netsh");
                    cycleExecutor = new CycleExecutor(() -> {
                        sendMessageToAgent(command.command(), -1);
                    }, json.getInt("delay"));
                    cycleExecutor.start();
                    sendMessageToClient("cycle started", false);
                } else if (message.equals("update-commands")) {
                    updateCommands(Variables.updateVariables());
                } else if (commands.containsKey(message)) {
                    Command command = commands.get(message);
                    ResultWaiter waiter = new ResultWaiter(lastId, command.result());
                    if (sendMessageToAgent(command.command(), lastId)) {
                        waiters.put(lastId, waiter);
                        waiter.start();
                    }
                    lastId++;
                }
            }
        } catch (IOException e) {
            onError(session, e);
        }
    }

    @OnClose
    public synchronized void onClose(Session session) {
        if (isKnownSession(session, agentSession))
            agentSession = null;
        if (isKnownSession(session, clientSession))
            clientSession = null;
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        synchronized (log) {
            log.accept(new Throwable(session.toString(), throwable));
        }
    }

    private boolean isKnownSession(Session check, Session target) {
        if (target == null)
            return false;
        return check.getId().equals(target.getId());
    }

    public synchronized void waitingInterrupt(int id, String message) {
        waiters.remove(id);
        sendMessageToClient(message, false);
    }

    public synchronized void waitingError(int id) {
        waiters.remove(id);
        sendMessageToClient("no response from agent", true);
    }

    private void sendMessageToClient(String message, boolean error) {
        if (clientSession == null)
            return;
        JSONObject json = new JSONObject();
        json.put("alert", message);
        json.put("status", error ? "error" : "success");
        try {
            clientSession.getBasicRemote().sendText(json.toString());
        } catch (IOException e) {
            onError(clientSession, e);
        }
    }

    private boolean sendMessageToAgent(String command, int id) {
        if (agentSession == null) {
            sendMessageToClient("agent not connected", true);
            return false;
        }
        JSONObject json = new JSONObject();
        json.put("command", command);
        json.put("id", id);
        try {
            agentSession.getBasicRemote().sendText(json.toString());
        } catch (IOException e) {
            onError(agentSession, e);
        }
        return true;
    }
}
