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
            WebSocketServer.waitingInterrupt(id, message);
            return;
        }
        WebSocketServer.waitingError(id);
    }

    public void start() {
        thread.start();
    }

    public void interrupt() {
        thread.interrupt();
    }
}
