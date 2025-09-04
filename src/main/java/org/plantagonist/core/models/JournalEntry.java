package org.plantagonist.core.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class JournalEntry {
    private String id;
    private String userId;
    private String plantId;
    private String plantName;
    private LocalDateTime entryDate;
    private String content;
    private String photoPath;

    public JournalEntry() {
        this.id = UUID.randomUUID().toString();
        this.entryDate = LocalDateTime.now();
    }

    public JournalEntry(String id, String userId, String plantId, String plantName,
                        LocalDateTime entryDate, String content, String photoPath) {
        this.id = id;
        this.userId = userId;
        this.plantId = plantId;
        this.plantName = plantName;
        this.entryDate = entryDate;
        this.content = content;
        this.photoPath = photoPath;
    }

    // Getters and Setters for MongoDB serialization
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPlantId() { return plantId; }
    public void setPlantId(String plantId) { this.plantId = plantId; }

    public String getPlantName() { return plantName; }
    public void setPlantName(String plantName) { this.plantName = plantName; }

    public LocalDateTime getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDateTime entryDate) { this.entryDate = entryDate; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    // Helper method to get formatted date
    public String getFormattedDate() {
        return entryDate.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    }

    // Helper method to get content preview
    public String getContentPreview() {
        if (content == null || content.isEmpty()) {
            return "No content";
        }
        String preview = content;
        if (preview.length() > 100) {
            preview = preview.substring(0, 100) + "...";
        }
        return preview;
    }
}