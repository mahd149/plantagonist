package org.plantagonist.core.models;


public class Plant {
    private String id; // uuid
    private String name;
    private String species;
    private int waterEveryDays; // schedule
    private double sunlightHoursPerDay;
    private String photoPath; // store path only


    public Plant() {}


    public Plant(String id, String name, String species, int waterEveryDays, double sunlightHoursPerDay, String photoPath) {
        this.id = id; this.name = name; this.species = species; this.waterEveryDays = waterEveryDays;
        this.sunlightHoursPerDay = sunlightHoursPerDay; this.photoPath = photoPath;
    }
// getters/setters omitted for brevity
}