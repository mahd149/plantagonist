package org.plantagonist.core.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GeoService {

    public static class LatLng {
        public double lat;
        public double lng;
    }

    private static String env(String key) {
        String v = System.getenv(key);
        return v == null ? "" : v.trim();
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Geocodes a city name via OpenCage. Returns null if nothing found. */
    public LatLng geocodeCity(String city) {
        try {
            String key = env("OPENCAGE_API_KEY");
            if (key.isBlank()) throw new IllegalStateException("OPENCAGE_API_KEY is not set");

            String q = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = "https://api.opencagedata.com/geocode/v1/json?q=" + q +
                    "&key=" + key + "&limit=1&no_annotations=1";

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new RuntimeException("OpenCage HTTP " + res.statusCode());
            }

            JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
            JsonArray results = root.getAsJsonArray("results");
            if (results == null || results.size() == 0) return null;

            JsonObject geometry = results.get(0).getAsJsonObject().getAsJsonObject("geometry");
            LatLng ll = new LatLng();
            ll.lat = geometry.get("lat").getAsDouble();
            ll.lng = geometry.get("lng").getAsDouble();
            return ll;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
