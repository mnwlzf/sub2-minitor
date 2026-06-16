package com.sub2.monitor.controller;

import com.sub2.monitor.scheduler.QuartzTaskFacade;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/quartz")
public class QuartzController {

    private final QuartzTaskFacade quartzTaskFacade;

    public QuartzController(QuartzTaskFacade quartzTaskFacade) {
        this.quartzTaskFacade = quartzTaskFacade;
    }

    @PostMapping("/collection/start")
    public void startCollection(@RequestParam String cron) {
        quartzTaskFacade.startBalanceCollection(cron);
    }

    @PostMapping("/collection/run")
    public void runCollectionNow() {
        quartzTaskFacade.runBalanceCollectionNow();
    }

    @PostMapping("/collection/pause")
    public void pauseCollection() {
        quartzTaskFacade.pauseBalanceCollection();
    }

    @PostMapping("/collection/resume")
    public void resumeCollection() {
        quartzTaskFacade.resumeBalanceCollection();
    }

    @PostMapping("/collection/remove")
    public void removeCollection() {
        quartzTaskFacade.removeBalanceCollection();
    }

    @GetMapping("/collection/status")
    public Map<String, Object> status() throws SchedulerException {
        return Map.of("scheduled", quartzTaskFacade.isBalanceCollectionActive());
    }
}
