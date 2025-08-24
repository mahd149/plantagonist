package org.plantagonist.core.models;

public class UserProfile {
    // Stored in user.json (via Gson)
    private String city;     // e.g., "Dhaka"
    private String units;    // "metric" or "imperial"

    public UserProfile() {
        // sensible defaults
        this.city = "Dhaka";
        this.units = "metric";
    }

    public UserProfile(String city, String units) {
        this.city = city;
        this.units = units;
    }

    /** Used by MainController to geocode & fetch weather. */
    public String getCity() {
        return (city == null || city.isBlank()) ? "Dhaka" : city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    /** "metric" → °C/mm, "imperial" → °F/in */
    public String getUnits() {
        return (units == null || units.isBlank()) ? "metric" : units;
    }

    public void setUnits(String units) {
        this.units = units;
    }
}
