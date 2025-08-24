package org.plantagonist.core.services;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.Instant;
import java.util.StringJoiner;

public class DiagnosticsService {

    // --- helpers to read env vars safely ---
    private static String env(String key) {
        String v = System.getenv(key);
        return v == null ? "" : v.trim();
    }
    private static boolean isSet(String key) {
        return !env(key).isBlank();
    }
    private static String check(String label, boolean ok) {
        return label + ": " + (ok ? "PASS" : "MISSING");
    }

    public static String runAll(String city) {
        StringJoiner out = new StringJoiner("\n");
        out.add("== Plantagonist Diagnostics @ " + Instant.now() + " ==");
        out.add("");

        // --- keys/URIs from ENV ONLY ---
        boolean hasOC = isSet("OPENCAGE_API_KEY");
        boolean hasWA = !env("WEATHERAPI_KEY").isBlank() || !env("WEATHER_API_KEY").isBlank() || !env("OPENWEATHER_API_KEY").isBlank();
        out.add("WeatherAPI key: " + (hasWA ? "PASS" : "MISSING"));

        String mongoUri = env("MONGODB_URI");
        String mongoDb  = env("MONGODB_DB");

        out.add(check("OpenCage key", hasOC));
        out.add(check("WeatherAPI key", hasWA));
        out.add(check("MongoDB URI", !mongoUri.isBlank()) + (mongoUri.isBlank() ? "" : "  (" + mongoUri + ")"));
        out.add(check("MongoDB DB",  !mongoDb.isBlank())  + (mongoDb.isBlank()  ? "" : "  (" + mongoDb  + ")"));
        out.add("");

        // --- Mongo ping ---
        try {
            MongoDatabase db = org.plantagonist.core.db.MongoConfig.db();
            Document r = db.runCommand(new Document("ping", 1));
            out.add("Mongo ping: PASS " + r.toJson());
        } catch (Exception e) {
            out.add("Mongo ping: FAIL " + e.getClass().getSimpleName() + " — " + e.getMessage());
        }

        // --- Geocode & Weather (uses your city) ---
        GeoService.LatLng ll = null;
        try {
            GeoService geo = new GeoService();
            ll = geo.geocodeCity(city);
            out.add("Geocoding (" + city + "): " + (ll != null ? ("PASS lat=" + ll.lat + ", lon=" + ll.lng) : "FAIL (no result)"));
        } catch (Exception e) {
            out.add("Geocoding (" + city + "): FAIL " + e.getMessage());
        }

        if (ll != null) {
            try {
                WeatherService ws = new WeatherService();
                WeatherService.WeatherNow w = ws.getNow(ll.lat, ll.lng);
                out.add(String.format("Weather: PASS %.1f°C, precip %.2f mm, %s", w.tempC, w.precipMm, w.description));
            } catch (Exception e) {
                out.add("Weather: FAIL " + e.getMessage());
            }
        }

        return out.toString();
    }
}
