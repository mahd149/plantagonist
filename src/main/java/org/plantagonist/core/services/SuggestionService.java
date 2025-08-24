package org.plantagonist.core.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Produces watering guidance based on weather, schedule, and optional soil moisture.
 * - Keep using waterAdvice(precipMm, tempC) for quick advice (string).
 * - Use advise(...) for a structured decision (action + message + notes + next check date).
 */
public class SuggestionService {

    // --- Tunables (conservative defaults for common houseplants) ---
    private static final double RAIN_SKIP_THRESHOLD_MM = 2.0;  // if >= mm in last 1h/3h/24h -> likely skip
    private static final double HOT_DAY_C = 34.0;              // hot threshold
    private static final double COOL_DAY_C = 18.0;             // cool threshold

    private static final int    DEFAULT_WATER_EVERY_DAYS = 7;  // used if schedule not provided
    private static final int    CHECK_SOIL_IN_DAYS = 1;        // when we advise check soil, re-check soon

    // Soil moisture heuristics (manual entry)
    private static final double MOISTURE_LOW_PCT = 25.0;       // <= water now
    private static final double MOISTURE_CAUTION_PCT = 40.0;   // <= check soil; > skip

    /** Quick, string-only advice (backward compatible). */
    public String waterAdvice(double precipMm, double tempC) {
        if (precipMm >= RAIN_SKIP_THRESHOLD_MM) {
            return "It rained recently — skip watering today.";
        }
        if (tempC >= HOT_DAY_C) {
            return "Hot day — check soil; likely water needed.";
        }
        if (tempC <= COOL_DAY_C) {
            return "Cool day — soil dries slower; follow schedule.";
        }
        return "Normal conditions — follow schedule.";
    }

    // ---------- Rich advice API ----------

    public enum Action { WATER_NOW, CHECK_SOIL, SKIP_TODAY }

    public static final class Advice {
        public Action action;
        public String message;
        public List<String> notes = new ArrayList<>();
        public LocalDate nextCheck;     // when to check again (if action == CHECK_SOIL or SKIP)
        public double confidence;       // 0.0 - 1.0
    }

    /**
     * Structured advice using weather + schedule + last watering + optional soil moisture.
     *
     * @param weather         current weather snapshot (tempC, precipMm)
     * @param lastWatered     last watering date (nullable)
     * @param waterEveryDays  schedule interval in days (nullable -> default 7)
     * @param soilMoisturePct optional % (0-100). If provided, overrides weather bias.
     * @param droughtTolerant true for cacti/succulents – more conservative watering
     */
    public Advice advise(WeatherService.WeatherNow weather,
                         LocalDate lastWatered,
                         Integer waterEveryDays,
                         Double soilMoisturePct,
                         boolean droughtTolerant) {

        Advice a = new Advice();
        int interval = coalesce(waterEveryDays, DEFAULT_WATER_EVERY_DAYS);
        int daysSince = (lastWatered == null) ? interval : (int) Math.max(0, ChronoUnit.DAYS.between(lastWatered, LocalDate.now()));
        boolean dueBySchedule = daysSince >= interval;

        // 1) Strong signals first
        if (soilMoisturePct != null) {
            if (soilMoisturePct <= (droughtTolerant ? MOISTURE_LOW_PCT - 5 : MOISTURE_LOW_PCT)) {
                a.action = Action.WATER_NOW;
                a.message = "Soil moisture is low — water now.";
                a.notes.add("Soil moisture " + fmtPct(soilMoisturePct) + "%.");
                attachWeatherNotes(a, weather);
                a.confidence = 0.95;
                return a;
            }
            if (soilMoisturePct <= (droughtTolerant ? MOISTURE_CAUTION_PCT - 5 : MOISTURE_CAUTION_PCT)) {
                a.action = Action.CHECK_SOIL;
                a.message = "Borderline moisture — check soil depth; water if top 2–3 cm are dry.";
                a.notes.add("Soil moisture " + fmtPct(soilMoisturePct) + "%.");
                attachWeatherNotes(a, weather);
                a.nextCheck = LocalDate.now().plusDays(CHECK_SOIL_IN_DAYS);
                a.confidence = 0.75;
                return a;
            }
            // Moisture is healthy
            a.action = Action.SKIP_TODAY;
            a.message = "Soil moisture is healthy — skip watering today.";
            a.notes.add("Soil moisture " + fmtPct(soilMoisturePct) + "%.");
            attachWeatherNotes(a, weather);
            a.nextCheck = computeNextCheck(lastWatered, interval, weather, droughtTolerant);
            a.confidence = 0.9;
            return a;
        }

        // 2) Weather-based adjustments (no moisture reading)
        if (weather != null && weather.precipMm >= RAIN_SKIP_THRESHOLD_MM) {
            a.action = Action.SKIP_TODAY;
            a.message = "Recent rain — skip watering today.";
            attachScheduleNotes(a, dueBySchedule, daysSince, interval);
            a.nextCheck = computeNextCheck(lastWatered, interval, weather, droughtTolerant);
            a.confidence = 0.85;
            return a;
        }

        // 3) Schedule + heat bias
        if (dueBySchedule) {
            if (weather != null && weather.tempC >= HOT_DAY_C) {
                a.action = Action.CHECK_SOIL;
                a.message = "Scheduled watering due; hot day — check soil and water if dry.";
                a.notes.add("It’s hot (≥ " + (int) HOT_DAY_C + "°C).");
                a.confidence = 0.8;
            } else {
                a.action = Action.CHECK_SOIL;
                a.message = "Scheduled watering due — check soil; water if top 2–3 cm are dry.";
                a.confidence = 0.7;
            }
            a.nextCheck = LocalDate.now().plusDays(CHECK_SOIL_IN_DAYS);
            return a;
        }

        // 4) Not yet due — provide conservative guidance
        a.action = Action.SKIP_TODAY;
        a.message = "Not yet due by schedule — skip today.";
        attachScheduleNotes(a, false, daysSince, interval);
        attachWeatherNotes(a, weather);
        a.nextCheck = computeNextCheck(lastWatered, interval, weather, droughtTolerant);
        a.confidence = 0.7;
        return a;
    }

    /** Heuristic next check date considering heat/rain & drought tolerance. */
    public LocalDate computeNextCheck(LocalDate lastWatered,
                                      int intervalDays,
                                      WeatherService.WeatherNow w,
                                      boolean droughtTolerant) {
        int adjust = 0;
        if (w != null) {
            if (w.tempC >= HOT_DAY_C) adjust += 1;                 // check sooner in heat
            if (w.precipMm >= RAIN_SKIP_THRESHOLD_MM) adjust -= 1; // check later after rain
        }
        if (droughtTolerant) adjust -= 1; // cacti/succulents: slower schedule
        int days = Math.max(1, intervalDays - Math.max(0, adjust));
        LocalDate base = (lastWatered == null) ? LocalDate.now() : lastWatered;
        return base.plusDays(days);
    }

    // ---------- helpers ----------

    private static int coalesce(Integer v, int d) { return v == null ? d : v; }

    private static String fmtPct(double v) {
        if (v < 0) v = 0;
        if (v > 100) v = 100;
        return String.format("%.0f", v);
    }

    private static void attachWeatherNotes(Advice a, WeatherService.WeatherNow w) {
        if (w == null) return;
        a.notes.add(String.format("Weather: %.1f°C, precip %.2f mm%s",
                w.tempC, w.precipMm, w.description != null ? " (" + w.description + ")" : ""));
        if (w.tempC >= HOT_DAY_C) a.notes.add("High temperature increases evaporation.");
        if (w.tempC <= COOL_DAY_C) a.notes.add("Cool weather slows drying.");
        if (w.precipMm >= RAIN_SKIP_THRESHOLD_MM) a.notes.add("Recent precipitation keeps soil moist longer.");
    }

    private static void attachScheduleNotes(Advice a, boolean dueBySchedule, int daysSince, int interval) {
        a.notes.add("Schedule: every " + interval + " days; last watered " +
                (daysSince == 0 ? "today" : daysSince + " day(s) ago") + (dueBySchedule ? " (due)" : " (not due)"));
    }
}
