package com.vmware.mapbu.support.jmw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping("/api/v1/load")
public class LoadSimulationController {
    private static final Logger log = LoggerFactory.getLogger(LoadSimulationController.class);

    @GetMapping("/wait")
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    public void waitingLoad(@RequestParam(defaultValue = "1") int howLong) throws InterruptedException {
        log.info("waiting for " + howLong + " seconds");
        Thread.sleep(howLong * 1000);
    }

    @GetMapping("/busy")
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    public void waitingBusy(@RequestParam(defaultValue = "1") int howLong) {
        log.info("spinning for " + howLong + " seconds");
        long sleepTime = howLong * 1000000000L; // convert to nanoseconds
        long startTime = System.nanoTime();
        while ((System.nanoTime() - startTime) < sleepTime) {
        }
    }
}