package com.sub2.monitor.scheduler.controller;

import com.sub2.monitor.scheduler.entity.SchedulerTask;
import com.sub2.monitor.scheduler.service.SchedulerTaskService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scheduler/tasks")
public class SchedulerTaskController {

    private final SchedulerTaskService schedulerTaskService;

    public SchedulerTaskController(SchedulerTaskService schedulerTaskService) {
        this.schedulerTaskService = schedulerTaskService;
    }

    @GetMapping
    public List<SchedulerTask> listTasks() {
        return schedulerTaskService.listTasks();
    }

    @PostMapping
    public SchedulerTask createTask(@RequestBody SchedulerTask task) {
        return schedulerTaskService.createTask(task);
    }

    @PutMapping("/{id}")
    public SchedulerTask updateTask(@PathVariable Long id, @RequestBody SchedulerTask task) {
        return schedulerTaskService.updateTask(id, task);
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long id) {
        schedulerTaskService.deleteTask(id);
    }

    @PostMapping("/{id}/pause")
    public void pauseTask(@PathVariable Long id) {
        schedulerTaskService.pauseTask(id);
    }

    @PostMapping("/{id}/resume")
    public void resumeTask(@PathVariable Long id) {
        schedulerTaskService.resumeTask(id);
    }

    @PostMapping("/{id}/trigger")
    public void triggerTask(@PathVariable Long id) {
        schedulerTaskService.triggerTask(id);
    }

    @PostMapping("/sync")
    public void syncTasks() {
        schedulerTaskService.syncAllEnabledTasks();
    }
}
