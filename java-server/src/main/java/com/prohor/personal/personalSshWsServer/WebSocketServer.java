package com.prohor.personal.personalSshWsServer;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;
import org.slf4j.*;

import java.io.*;
import java.util.*;

@ServerEndpoint(value = "/")
public class WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    private static Session agentSession;
    private static Session clientSession;
    private static final Set<String> newSessions = new HashSet<>();
    private static final Map<String, Command> commands = new HashMap<>();
    private static final Map<Integer, ResultWaiter> waiters = new HashMap<>();
    private static int lastId = 0;
    private static CycleExecutor cycleExecutor;

    static {
        try {
            updateCommands(Variables.getVariables());
        } catch (IOException e) {
            log.error("Error updating commands", e);
        }
    }

    private static void updateCommands(JSONObject var) {
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
                log.warn("too many new sessions, {} close", hash(session));
                session.close();
                return;
            }
            log.info("session {} added to new", hash(session));
            newSessions.add(session.getId());
            JSONObject object = new JSONObject();
            object.put("command", "ping");
            object.put("alert", "ping");
            object.put("id", -1);
            session.getBasicRemote().sendText(object.toString());
        } catch (IOException e) {
            onError(session, e);
        }
    }

    @OnMessage
    public synchronized void onMessage(Session session, String message) {
        if (newSessions.contains(session.getId())) {
            newSessions.remove(session.getId());
            if (message.contains("rust-pong")) {
                agentSession = session;
                log.info("agent session established: {}", hash(session));
            } else if (message.equals("js-pong")) {
                clientSession = session;
                log.info("client session established: {}", hash(session));
            } else
                log.warn("unknown answer from new session {}: {}", hash(session), message);
            return;
        }
        JSONObject json = new JSONObject(message);
        try {
            if (isKnownSession(session, agentSession)) {
                log.info("message from agent {}: {}", hash(session), json);
                int id = json.getInt("id");
                if (waiters.containsKey(id)) {
                    ResultWaiter waiter = waiters.get(id);
                    waiter.interrupt();
                }
                return;
            }
            if (!isKnownSession(session, clientSession)) {
                log.warn("message from unknown session {}: {}", hash(session), json);
                return;
            }
            log.info("message from client {}: {}", hash(session), json);
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
                cycleExecutor = new CycleExecutor(
                        () -> sendMessageToAgent(command.command(), -1), json.getInt("delay")
                );
                cycleExecutor.start();
                sendMessageToClient("cycle started", false);
            } else if (message.equals("update-commands")) {
                updateCommands(Variables.updateVariables());
                sendMessageToClient("commands was updated", false);
            } else if (commands.containsKey(message)) {
                Command command = commands.get(message);
                ResultWaiter waiter = new ResultWaiter(lastId, command.result());
                if (sendMessageToAgent(command.command(), lastId)) {
                    waiters.put(lastId, waiter);
                    waiter.start();
                }
                lastId++;
            }
        } catch (IOException e) {
            onError(session, e);
        }
    }

    @OnClose
    public synchronized void onClose(Session session) {
        newSessions.remove(session.getId());
        if (isKnownSession(session, agentSession)) {
            agentSession = null;
            log.debug("agent {} disconnected", hash(session));
        } else if (isKnownSession(session, clientSession)) {
            clientSession = null;
            log.debug("client {} disconnected", hash(session));
        } else
            log.debug("unknown session {} disconnected", hash(session));
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        synchronized (log) {
            log.error("Error in session {}", hash(session), throwable);
        }
    }

    private boolean isKnownSession(Session check, Session target) {
        if (target == null)
            return false;
        return check.getId().equals(target.getId());
    }

    public static synchronized void waitingInterrupt(int id, String message) {
        waiters.remove(id);
        sendMessageToClient(message, false);
    }

    public static synchronized void waitingError(int id) {
        waiters.remove(id);
        sendMessageToClient("no response from agent", true);
    }

    private static void sendMessageToClient(String message, boolean error) {
        if (clientSession == null)
            return;
        JSONObject json = new JSONObject();
        json.put("alert", message);
        json.put("status", error ? "error" : "success");
        try {
            clientSession.getBasicRemote().sendText(json.toString());
        } catch (IOException e) {
            log.warn("error send message to client", e);
        }
    }

    private static boolean sendMessageToAgent(String command, int id) {
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
            log.warn("error send message to agent", e);
        }
        return true;
    }

    private static String hash(Session session) {
        String s = Integer.toHexString(session.getId().hashCode());
        return "0".repeat(8 - s.length()) + s;
    }
}
