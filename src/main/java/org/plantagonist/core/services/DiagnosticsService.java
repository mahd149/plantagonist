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

        // Add WeatherAPI-specific test
        out.add("");
        out.add("=".repeat(50));
        out.add(testWeatherApiConnection(city));

        return out.toString();
    }

    public static String testWeatherApiConnection(String city) {
        StringJoiner out = new StringJoiner("\n");
        out.add("== WeatherAPI.com Connection Test @ " + Instant.now() + " ==");
        out.add("");

        // Check if API key is available
        String weatherApiKey = env("WEATHERAPI_KEY");
        boolean hasKey = !weatherApiKey.isBlank();

        out.add("WEATHERAPI_KEY: " + (hasKey ? "PRESENT" : "MISSING"));
        if (hasKey) {
            out.add("Key preview: " + weatherApiKey.substring(0, Math.min(8, weatherApiKey.length())) + "..." +
                    weatherApiKey.substring(weatherApiKey.length() - 4));
        }
        out.add("");

        if (!hasKey) {
            out.add("❌ Cannot test WeatherAPI connection - missing API key");
            out.add("Set WEATHERAPI_KEY environment variable with your WeatherAPI.com key");
            return out.toString();
        }

        try {
            // Test geocoding first (to get coordinates)
            out.add("📍 Geocoding test for: " + city);
            GeoService geo = new GeoService();
            GeoService.LatLng coordinates = geo.geocodeCity(city);

            if (coordinates == null) {
                out.add("❌ Geocoding failed - cannot get coordinates for: " + city);
                return out.toString();
            }

            out.add("✅ Geocoding successful: lat=" + coordinates.lat + ", lon=" + coordinates.lng);
            out.add("");

            // Test WeatherAPI connection
            out.add("🌤️  Testing WeatherAPI connection...");
            WeatherService weatherService = new WeatherService();
            WeatherService.WeatherNow weather = weatherService.getNow(coordinates.lat, coordinates.lng);

            out.add("✅ WeatherAPI connection successful!");
            out.add("");
            out.add("📊 Weather data received:");
            out.add("  Temperature: " + weather.tempC + "°C");
            out.add("  Precipitation: " + weather.precipMm + " mm");
            out.add("  Condition: " + weather.description);
            out.add("  Humidity: " + (weather.humidity != null ? weather.humidity + "%" : "N/A"));
            out.add("  Wind: " + (weather.windKph != null ? weather.windKph + " km/h" : "N/A"));
            out.add("  Location: " + (weather.locationName != null ? weather.locationName : "N/A"));

            if (weather.lastUpdated != null) {
                out.add("  Last updated: " + weather.lastUpdated);
            }

            out.add("");
            out.add("🎉 WeatherAPI.com connection test: PASS");

        } catch (Exception e) {
            out.add("");
            out.add("❌ WeatherAPI connection test: FAILED");
            out.add("Error: " + e.getClass().getSimpleName());
            out.add("Message: " + e.getMessage());

            // Provide troubleshooting tips
            out.add("");
            out.add("🔧 Troubleshooting tips:");
            out.add("1. Verify your WEATHERAPI_KEY is correct");
            out.add("2. Check your internet connection");
            out.add("3. Ensure WeatherAPI.com service is available");
            out.add("4. Verify the API key has proper permissions");
            out.add("5. Check if you've exceeded API rate limits");
        }

        return out.toString();
    }
}
