package com.vmware.mapbu.support.jmw;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimplePool {
    private static final Logger log = LoggerFactory.getLogger(SimplePool.class);

    private Map<UUID, Permit> available = new HashMap<>(5);
    private Map<UUID, Permit> inUse = new HashMap<>(5);
    private final Semaphore lock = new Semaphore(5, false);

    public SimplePool() {
        for (int i = 0; i < 5; i++) {
            UUID id = UUID.randomUUID();
            available.put(id, new Permit(id, i));
        }
    }

    public String metrics() {
        return "available -> " + available.size() + " -- in use -> " + inUse.size() + " -- waiting -> "
                + lock.getQueueLength();
    }

    public Permit obtainPermit() throws InterruptedException {
        log.info("Obtaining permit (" + metrics() + ")");
        lock.acquire();
        synchronized (SimplePool.class) {
            Permit p = available.values().iterator().next();
            available.remove(p.getId());
            inUse.put(p.getId(), p);
            log.info("Acquired permit (" + metrics() + ")");
            return p;
        }
    }

    public synchronized void returnPermit(Permit p) throws InterruptedException {
        synchronized (SimplePool.class) {
            log.info("Freeing permit (" + metrics() + ")");
            if (!inUse.containsKey(p.getId())) {
                throw new RuntimeException("Returned a permit that is not in use");
            }
            available.put(p.getId(), inUse.remove(p.getId()));
        }
        lock.release();
        log.info("Returned permit (" + metrics() + ")");
    }

}
