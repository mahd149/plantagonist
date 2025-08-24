package org.plantagonist.core.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

public class WeatherService {

    public static class WeatherNow {
        public double tempC;        // current temperature (°C)
        public double precipMm;     // precipitation (mm)
        public String description;  // e.g., "Partly cloudy"

        // NEW: where this reading is for
        public Double lat;
        public Double lon;
        public String locationName; // e.g., "Dhaka"
    }

    private static String env(String key) {
        String v = System.getenv(key);
        return v == null ? "" : v.trim();
    }

    private static String weatherApiKey() {
        String k = env("WEATHERAPI_KEY");
        if (k.isBlank()) k = env("WEATHER_API_KEY");
        return k;
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** AUTO: detect by caller IP (no OpenCage needed). */
    public WeatherNow getNowAuto() {
        try {
            String key = weatherApiKey();
            if (key.isBlank()) throw new IllegalStateException("WEATHERAPI_KEY is not set");

            String url = "https://api.weatherapi.com/v1/current.json?key="
                    + URLEncoder.encode(key, StandardCharsets.UTF_8)
                    + "&q=auto:ip&aqi=no";

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new RuntimeException("WeatherAPI HTTP " + res.statusCode() + ": " + res.body());
            }

            JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
            if (root.has("error")) {
                String msg = root.getAsJsonObject("error").get("message").getAsString();
                throw new RuntimeException("WeatherAPI error: " + msg);
            }

            JsonObject loc = root.getAsJsonObject("location");
            JsonObject cur = root.getAsJsonObject("current");

            WeatherNow w = new WeatherNow();
            w.tempC = cur.get("temp_c").getAsDouble();
            w.precipMm = cur.has("precip_mm") ? cur.get("precip_mm").getAsDouble() : 0.0;
            w.description = cur.getAsJsonObject("condition").get("text").getAsString();
            w.lat = loc.get("lat").getAsDouble();
            w.lon = loc.get("lon").getAsDouble();
            w.locationName = loc.get("name").getAsString();
            return w;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Manual: fetch by lat/lon (used when city is set in Settings). */
    public WeatherNow getNow(double lat, double lon) {
        try {
            String key = weatherApiKey();
            if (key.isBlank()) throw new IllegalStateException("WEATHERAPI_KEY is not set");

            String url = String.format(Locale.ROOT,
                    "https://api.weatherapi.com/v1/current.json?key=%s&q=%f,%f&aqi=no",
                    URLEncoder.encode(key, StandardCharsets.UTF_8), lat, lon);

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new RuntimeException("WeatherAPI HTTP " + res.statusCode() + ": " + res.body());
            }

            JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
            if (root.has("error")) {
                String msg = root.getAsJsonObject("error").get("message").getAsString();
                throw new RuntimeException("WeatherAPI error: " + msg);
            }

            JsonObject loc = root.getAsJsonObject("location");
            JsonObject cur = root.getAsJsonObject("current");

            WeatherNow w = new WeatherNow();
            w.tempC = cur.get("temp_c").getAsDouble();
            w.precipMm = cur.has("precip_mm") ? cur.get("precip_mm").getAsDouble() : 0.0;
            w.description = cur.getAsJsonObject("condition").get("text").getAsString();
            w.lat = loc.get("lat").getAsDouble();
            w.lon = loc.get("lon").getAsDouble();
            w.locationName = loc.get("name").getAsString();
            return w;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
