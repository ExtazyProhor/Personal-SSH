package com.prohor.personal.personalSshWsServer;

public class CycleExecutor extends Thread {
    private final Task task;
    private final int delay;

    public CycleExecutor(Task task, int delay) {
        super(CycleExecutor.class.getName());
        this.task = task;
        this.delay = delay;
    }

    @Override
    @SuppressWarnings("all")
    public void run() {
        while (true) {
            try {
                Thread.sleep(delay * 1000L);
            } catch (InterruptedException e) {
                break;
            }
            task.complete();
        }
    }
}
