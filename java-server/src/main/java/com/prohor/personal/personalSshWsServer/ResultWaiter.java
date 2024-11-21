package com.prohor.personal.personalSshWsServer;

public class ResultWaiter implements Runnable {
    private final int id;
    private final String message;
    private final Thread thread;

    public ResultWaiter(int id, String message) {
        this.id = id;
        this.message = message;
        this.thread = new Thread(this);
    }

    @Override
    public void run() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            WebSocketServer.webSocketServer.waitingInterrupt(id, message);
            return;
        }
        WebSocketServer.webSocketServer.waitingError(id);
    }

    public void start() {
        thread.start();
    }

    public void interrupt() {
        thread.interrupt();
    }
}
