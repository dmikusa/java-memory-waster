package com.vmware.mapbu.support.jmw;

import java.util.Random;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Philosopher extends Thread {
    private static final Logger log = LoggerFactory.getLogger(Philosopher.class);
    private static int num = 0;

    private Lock left;
    private Lock right;
    private Random random = new Random();

    public Philosopher(Lock left, Lock right) {
        super("Philosopher-" + num);
        synchronized (Philosopher.class) {
            num++;
        }
        this.left = left;
        this.right = right;
    }

    private int randomSleepTime() {
        return random.nextInt(51) + 100;
    }

    @Override
    public void run() {
        log.info("I think therefore I am");
        try {
            while (true) {
                try {
                    left.lockInterruptibly();
                    Thread.yield();
                    right.lockInterruptibly();
                    log.info("I have the power and both forks! Time to eat.");
                    Thread.sleep(randomSleepTime());
                } finally {
                    log.info("I'm done eating");
                    left.unlock();
                    right.unlock();
                }
                log.info("I'm hungry again");
            }
        } catch (InterruptedException | IllegalMonitorStateException ex) {
            log.warn("Interrupted while getting a fork, how rude!");
            return;
        }
    }
}
