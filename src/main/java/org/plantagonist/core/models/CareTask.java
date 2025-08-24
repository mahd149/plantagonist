package org.plantagonist.core.models;
import java.time.LocalDate;


public class CareTask {
    private String plantId;
    private LocalDate dueDate;
    private String type; // WATER / FERTILIZE / SUN
    private boolean completed;
}