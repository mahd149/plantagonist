package org.plantagonist.core.models;

import java.time.LocalDate;

public class CareLogEntry {
    private String id;                 // uuid
    private String plantId;            // FK -> Plant.id
    /** Stored as ISO-8601 (yyyy-MM-dd) to avoid custom codecs */
    private String dateIso;
    private Double soilMoisturePct;    // nullable
    private Double fertilizerMl;       // nullable
    private String notes;              // nullable

    public CareLogEntry() {}

    public CareLogEntry(String id, String plantId, LocalDate date,
                        Double soilMoisturePct, Double fertilizerMl, String notes) {
        this.id = id;
        this.plantId = plantId;
        setDate(date);
        this.soilMoisturePct = soilMoisturePct;
        this.fertilizerMl = fertilizerMl;
        this.notes = notes;
    }

    // ----- getters & setters -----
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlantId() { return plantId; }
    public void setPlantId(String plantId) { this.plantId = plantId; }

    /** Underlying persisted field */
    public String getDateIso() { return dateIso; }
    public void setDateIso(String dateIso) { this.dateIso = dateIso; }

    public Double getSoilMoisturePct() { return soilMoisturePct; }
    public void setSoilMoisturePct(Double soilMoisturePct) { this.soilMoisturePct = soilMoisturePct; }

    public Double getFertilizerMl() { return fertilizerMl; }
    public void setFertilizerMl(Double fertilizerMl) { this.fertilizerMl = fertilizerMl; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // ----- convenience (not persisted) -----
    public LocalDate getDate() {
        return (dateIso == null || dateIso.isBlank()) ? null : LocalDate.parse(dateIso);
    }

    public void setDate(LocalDate date) {
        this.dateIso = (date == null) ? null : date.toString();
    }
}
