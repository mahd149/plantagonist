package org.plantagonist.core.models;

import java.time.LocalDate;

public class CareLogEntry {
    private String id;                 // uuid
    private String plantId;            // FK -> Plant.id
    private String userId;             // FK -> User.id
    /** Stored as ISO-8601 (yyyy-MM-dd) to avoid custom codecs */
    private String dateIso;
    private String actionType;         // "WATERING", "FERTILIZING", "SOIL_CHANGE", "OTHER"
    private Double soilMoisturePct;    // nullable
    private Double fertilizerMl;       // nullable
    private String notes;              // nullable
    private String plantName;          // denormalized for display

    public CareLogEntry() {}

    public CareLogEntry(String id, String plantId, String userId, LocalDate date, String actionType,
                        Double soilMoisturePct, Double fertilizerMl, String notes, String plantName) {
        this.id = id;
        this.plantId = plantId;
        this.userId = userId;
        setDate(date);
        this.actionType = actionType;
        this.soilMoisturePct = soilMoisturePct;
        this.fertilizerMl = fertilizerMl;
        this.notes = notes;
        this.plantName = plantName;
    }

    // ----- getters & setters -----
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlantId() { return plantId; }
    public void setPlantId(String plantId) { this.plantId = plantId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    /** Underlying persisted field */
    public String getDateIso() { return dateIso; }
    public void setDateIso(String dateIso) { this.dateIso = dateIso; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public Double getSoilMoisturePct() { return soilMoisturePct; }
    public void setSoilMoisturePct(Double soilMoisturePct) { this.soilMoisturePct = soilMoisturePct; }

    public Double getFertilizerMl() { return fertilizerMl; }
    public void setFertilizerMl(Double fertilizerMl) { this.fertilizerMl = fertilizerMl; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPlantName() { return plantName; }
    public void setPlantName(String plantName) { this.plantName = plantName; }

    // ----- convenience (not persisted) -----
    public LocalDate getDate() {
        return (dateIso == null || dateIso.isBlank()) ? null : LocalDate.parse(dateIso);
    }

    public void setDate(LocalDate date) {
        this.dateIso = (date == null) ? null : date.toString();
    }

    public String getActionTypeDisplay() {
        if (actionType == null) return "Care";
        switch (actionType) {
            case "WATERING": return "💧 Watering";
            case "FERTILIZING": return "🌿 Fertilizing";
            case "SOIL_CHANGE": return "🔄 Soil Change";
            default: return actionType;
        }
    }
}