package com.farm.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class ProcessRegistryService {

    private final Map<Long, Process> processes = new ConcurrentHashMap<>();

    public synchronized void registerProcess(Long cameraId, Process process) {
        processes.put(cameraId, process);
    }

    public synchronized boolean isRunning(Long cameraId) {
        return processes.containsKey(cameraId) && processes.get(cameraId).isAlive();
    }

    public synchronized boolean stopProcess(Long cameraId) {
        if (!processes.containsKey(cameraId)) {
            return false;
        }
        Process process = processes.remove(cameraId);
        if (process.isAlive()) {
            process.destroyForcibly();
            try {
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        return true;
    }

    public synchronized void cleanupDeadProcesses() {
        processes.entrySet().removeIf(entry -> !entry.getValue().isAlive());
    }

    @Scheduled(fixedDelay = 15000)
    public void purgeDeadProcesses() {
        cleanupDeadProcesses();
    }

    public synchronized void stopAllProcesses() {
        for (Long cameraId : java.util.List.copyOf(processes.keySet())) {
            stopProcess(cameraId);
        }
        processes.clear();
    }
}
