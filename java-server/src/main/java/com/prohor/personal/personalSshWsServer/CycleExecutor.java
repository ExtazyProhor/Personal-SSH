package com.prohor.personal.personalSshWsServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CycleExecutor extends Thread {
    private static final Logger log = LoggerFactory.getLogger(CycleExecutor.class);
    private final Task task;
    private final int delay;

    public CycleExecutor(Task task, int delay) {
        super(CycleExecutor.class.getName());
        this.task = task;
        this.delay = delay;
        log.info("cycle executor created, delay={}", delay);
    }

    @Override
    @SuppressWarnings("all")
    public void run() {
        while (true) {
            try {
                Thread.sleep(delay * 1000L);
            } catch (InterruptedException e) {
                log.info("cycle was interrupted");
                break;
            }
            task.complete();
            log.debug("cycle task completed");
        }
    }
}
