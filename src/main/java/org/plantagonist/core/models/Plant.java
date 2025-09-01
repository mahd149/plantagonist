package org.plantagonist.core.models;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

public class Plant {
    private final StringProperty userId = new SimpleStringProperty();

    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty species = new SimpleStringProperty();
    private final ObjectProperty<Integer> waterEveryDays = new SimpleObjectProperty<>();
    private final ObjectProperty<Double> sunlightHours = new SimpleObjectProperty<>();
    private final StringProperty photoPath = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> lastWatered = new SimpleObjectProperty<>();

    // Getters/Setters for JSON + TableView
    public String getId() { return id.get(); }
    public void setId(String v) { id.set(v); }
    public StringProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }
    public StringProperty nameProperty() { return name; }

    public String getSpecies() { return species.get(); }
    public void setSpecies(String v) { species.set(v); }
    public StringProperty speciesProperty() { return species; }

    public Integer getWaterEveryDays() { return waterEveryDays.get(); }
    public void setWaterEveryDays(Integer v) { waterEveryDays.set(v); }
    public ObjectProperty<Integer> waterEveryDaysProperty() { return waterEveryDays; }

    public Double getSunlightHours() { return sunlightHours.get(); }
    public void setSunlightHours(Double v) { sunlightHours.set(v); }
    public ObjectProperty<Double> sunlightHoursProperty() { return sunlightHours; }

    public String getPhotoPath() { return photoPath.get(); }
    public void setPhotoPath(String v) { photoPath.set(v); }
    public StringProperty photoPathProperty() { return photoPath; }

    public LocalDate getLastWatered() { return lastWatered.get(); }
    public void setLastWatered(LocalDate v) { lastWatered.set(v); }
    public ObjectProperty<LocalDate> lastWateredProperty() { return lastWatered; }


    // Add getters/setters
    public String getUserId() { return userId.get(); }
    public void setUserId(String v) { userId.set(v); }
    public StringProperty userIdProperty() { return userId; }

    public Plant copy() {
        Plant p = new Plant();
        p.setId(getId());
        p.setUserId(getUserId()); // ← ADD THIS
        p.setName(getName());
        p.setSpecies(getSpecies());
        p.setWaterEveryDays(getWaterEveryDays());
        p.setSunlightHours(getSunlightHours());
        p.setPhotoPath(getPhotoPath());
        p.setLastWatered(getLastWatered());
        return p;
    }

}