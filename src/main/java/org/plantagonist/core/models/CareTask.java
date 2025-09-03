package org.plantagonist.core.models;

import java.time.LocalDate;

public class CareTask {
    private String userId;
    private String id;           // UUID string (we'll set in code)
    private String plantId;      // links to Plant.id
    private String plantName;    // denormalized for display
    private LocalDate dueDate;   // when to do it
    private String type;         // "WATER", "FERTILIZE", "SOIL_CHANGE"
    private String status;       // "DUE", "TODAY", "UPCOMING", "DONE", "MISSED", "CANCELLED"
    private String notes;        // optional
    private Integer frequencyDays; // null for one-time tasks, number of days for recurring
    private LocalDate lastCompleted; // for recurring tasks

    // ---- getters/setters (Mongo POJO codec needs them) ----
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlantId() { return plantId; }
    public void setPlantId(String plantId) { this.plantId = plantId; }

    public String getPlantName() { return plantName; }
    public void setPlantName(String plantName) { this.plantName = plantName; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Integer getFrequencyDays() { return frequencyDays; }
    public void setFrequencyDays(Integer frequencyDays) { this.frequencyDays = frequencyDays; }

    public LocalDate getLastCompleted() { return lastCompleted; }
    public void setLastCompleted(LocalDate lastCompleted) { this.lastCompleted = lastCompleted; }

    public String getTypeDisplay() {
        if (type == null) return "Task";
        switch (type) {
            case "WATER": return "💧 Water";
            case "FERTILIZE": return "🌿 Fertilize";
            case "SOIL_CHANGE": return "🔄 Soil Change";
            default: return type;
        }
    }

    public String getStatusDisplay() {
        if (status == null) return "DUE";
        switch (status) {
            case "DUE": return "📅 Due";
            case "TODAY": return "⭐ Today";
            case "UPCOMING": return "🔜 Upcoming";
            case "DONE": return "✅ Done";
            case "MISSED": return "❌ Missed";
            case "CANCELLED": return "🚫 Cancelled";
            default: return status;
        }
    }

    public String getStatusColorClass() {
        if (status == null) return "status-due";
        switch (status) {
            case "DUE": return "status-due";
            case "TODAY": return "status-today";
            case "UPCOMING": return "status-upcoming";
            case "DONE": return "status-done";
            case "MISSED": return "status-missed";
            case "CANCELLED": return "status-cancelled";
            default: return "status-due";
        }
    }
}