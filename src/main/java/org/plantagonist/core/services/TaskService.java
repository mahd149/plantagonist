package org.plantagonist.core.services;

import org.plantagonist.core.models.CareTask;
import org.plantagonist.core.models.Plant;
import org.plantagonist.core.repositories.CareTaskRepository;
import org.plantagonist.core.repositories.PlantRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class TaskService {

    private final PlantRepository plantRepo;
    private final CareTaskRepository taskRepo;
    private final WeatherService weatherService;
    private final SuggestionService suggestion;

    public TaskService(PlantRepository plantRepo, CareTaskRepository taskRepo,
                       WeatherService weatherService, SuggestionService suggestion) {
        this.plantRepo = plantRepo;
        this.taskRepo = taskRepo;
        this.weatherService = weatherService;
        this.suggestion = suggestion;
    }

    /**
     * Recompute WATER tasks for all plants.
     * - next = lastWatered + waterEveryDays
     * - if next < today => DUE
     * - if next == today => TODAY (treated as DUE in ordering)
     * - else UPCOMING
     * Weather nudges:
     * - precip >= threshold => push by +1 day (unless already overdue)
     * - hot day => pull by -1 day (min today)
     */
    public void syncAllWaterTasks() {
        WeatherService.WeatherNow w = null;
        try {
            w = weatherService.getNowAuto();
        } catch (Exception ignored) {}

        List<Plant> plants = plantRepo.findAll();
        LocalDate today = LocalDate.now();

        for (Plant p : plants) {
            String pid = p.getId();
            if (pid == null || pid.isBlank()) continue;

            Integer every = p.getWaterEveryDays();
            LocalDate last = p.getLastWatered();

            // If no schedule, don’t create a task
            if (every == null || every <= 0) {
                // Also clean any existing WATER task for this plant
                taskRepo.deleteByPlantIdAndType(pid, "WATER");
                continue;
            }

            LocalDate next = (last == null) ? today.plusDays(every) : last.plusDays(every);

            // Weather nudges (conservative): only nudge future/today, not past-due
            if (w != null && (next.isAfter(today) || next.isEqual(today))) {
                // Roughly mirror your SuggestionService heuristics
                if (w.precipMm >= 2.0) {
                    next = next.plusDays(1);
                } else if (w.tempC >= 34.0) {
                    next = next.minusDays(1);
                    if (next.isBefore(today)) next = today;
                }
            }

            String status;
            if (next.isBefore(today)) status = "DUE";
            else if (next.isEqual(today)) status = "TODAY";
            else status = "UPCOMING";

            upsertWaterTask(pid, safe(p.getName()), next, status);
        }
    }

    private void upsertWaterTask(String plantId, String plantName, LocalDate due, String status) {
        // Delete any existing WATER task for this plant and insert fresh (keeps it simple)
        taskRepo.deleteByPlantIdAndType(plantId, "WATER");

        CareTask t = new CareTask();
        t.setId(UUID.randomUUID().toString());
        t.setPlantId(plantId);
        t.setPlantName(plantName);
        t.setType("WATER");
        t.setDueDate(due);
        t.setStatus(status);
        if ("DUE".equals(status) || "TODAY".equals(status)) {
            t.setNotes("Watering due");
        }
        taskRepo.insertOne(t);
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
