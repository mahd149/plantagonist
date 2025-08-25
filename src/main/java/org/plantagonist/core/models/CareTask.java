package org.plantagonist.core.models;

import java.time.LocalDate;

public class CareTask {
    private String id;           // UUID string (we'll set in code)
    private String plantId;      // links to Plant.id
    private String plantName;    // denormalized for display
    private LocalDate dueDate;   // when to do it
    private String type;         // "WATER" (future: "FERTILIZE", etc.)
    private String status;       // "DUE", "TODAY", "UPCOMING", "DONE"
    private String notes;        // optional

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
}
