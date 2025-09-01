package org.plantagonist.core.models;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * User profile / account settings.
 * Designed to work with local JSON (Gson) storage, but also fine as a Mongo POJO.
 *
 * Notes:
 * - We keep the original field name "ThemeMode" to avoid breaking any existing JSON.
 *   Use getThemeMode()/setThemeMode() to access it in code.
 * - id is a String UUID so this class can also serve as a Mongo document with _id:String if desired.
 */
public class UserProfile {

    // ---- Constants
    public static final String UNITS_METRIC   = "metric";   // °C, mm
    public static final String UNITS_IMPERIAL = "imperial"; // °F, in

    public static final String THEME_LIGHT  = "light";
    public static final String THEME_DARK   = "dark";
    public static final String THEME_SYSTEM = "system"; // optional

    // ---- Identity & auth
    private String id;           // UUID string; also usable as Mongo _id
    private String email;        // login (lowercased & trimmed)
    private String username;     // optional alternative login
    private String passwordHash; // bcrypt hash (never store plain text)

    // ---- Profile
    private String firstName;
    private String lastName;

    // ---- Preferences / settings (kept from your original class)
    private String city;         // e.g., "Dhaka"
    private String units;        // "metric" or "imperial"
    private String ThemeMode;    // "light" | "dark" | "system" (kept exact field name)

    // ---- Audit
    private Date createdAt;
    private Date updatedAt;

    // ---- Constructors

    /** Default: Dhaka, metric, light theme. */
    public UserProfile() {
        this.id = UUID.randomUUID().toString();
        this.city = "Dhaka";
        this.units = UNITS_METRIC;
        this.ThemeMode = THEME_LIGHT;
        Date now = new Date();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UserProfile(String city, String units) {
        this();
        this.city = city;
        this.units = units;
        normalize();
    }

    /** Convenience factory for registrations where you already computed a hash. */
    public static UserProfile create(String email, String username, String passwordHash) {
        UserProfile u = new UserProfile();
        u.setEmail(email);
        u.setUsername(username);
        u.setPasswordHash(passwordHash);
        u.touchCreated(); // sets createdAt/updatedAt if null
        u.normalize();
        return u;
    }

    // ---- Normalization / defaults

    /** Normalize fields (email case/trim, units/theme defaults, etc.). */
    public void normalize() {
        if (email != null) email = email.trim().toLowerCase();
        if (username != null) username = username.trim();

        // Units default
        if (units == null || units.isBlank()) units = UNITS_METRIC;
        String u = units.toLowerCase();
        if (!UNITS_METRIC.equals(u) && !UNITS_IMPERIAL.equals(u)) {
            units = UNITS_METRIC;
        } else {
            units = u;
        }

        // Theme default
        if (ThemeMode == null || ThemeMode.isBlank()) ThemeMode = THEME_LIGHT;
        String t = ThemeMode.toLowerCase();
        if (!THEME_LIGHT.equals(t) && !THEME_DARK.equals(t) && !THEME_SYSTEM.equals(t)) {
            ThemeMode = THEME_LIGHT;
        } else {
            ThemeMode = t;
        }

        // City default
        if (city == null || city.isBlank()) city = "Dhaka";

        // Timestamps
        if (createdAt == null) createdAt = new Date();
        if (updatedAt == null) updatedAt = new Date();
    }

    public void touchUpdated() {
        this.updatedAt = new Date();
    }

    public void touchCreated() {
        Date now = new Date();
        if (this.createdAt == null) this.createdAt = now;
        this.updatedAt = now;
    }

    // ---- Convenience

    /** "metric" → °C/mm, "imperial" → °F/in (never null). */
    public String getUnits() {
        return (units == null || units.isBlank()) ? UNITS_METRIC : units;
    }

    public boolean isMetric() { return UNITS_METRIC.equalsIgnoreCase(getUnits()); }

    /** City never null/blank; defaults to "Dhaka". */
    public String getCity() {
        return (city == null || city.isBlank()) ? "Dhaka" : city;
    }

    public String getDisplayName() {
        String fn = firstName == null ? "" : firstName.trim();
        String ln = lastName == null ? "" : lastName.trim();
        String full = (fn + " " + ln).trim();
        if (!full.isEmpty()) return full;
        if (username != null && !username.isBlank()) return username;
        return email != null ? email : "User";
    }

    // ---- Getters / Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    /** Lowercased on normalize(). */
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    /** Store only a bcrypt hash here. */
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public void setCity(String city) { this.city = city; }

    public void setUnits(String units) { this.units = units; }

    /** Accessor for the original field "ThemeMode". */
    public String getThemeMode() {
        return (ThemeMode == null || ThemeMode.isBlank()) ? THEME_LIGHT : ThemeMode;
    }

    /** Mutator for the original field "ThemeMode". */
    public void setThemeMode(String themeMode) {
        this.ThemeMode = themeMode;
    }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    // ---- Equality / Hashing (by id)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserProfile)) return false;
        UserProfile that = (UserProfile) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "UserProfile{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", city='" + city + '\'' +
                ", units='" + units + '\'' +
                ", ThemeMode='" + ThemeMode + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
