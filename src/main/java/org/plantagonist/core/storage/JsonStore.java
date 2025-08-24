package org.plantagonist.core.storage;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;


public class JsonStore {
    private static final Gson GSON = new Gson();


    public static <T> List<T> readList(Path path, Type typeOfT) {
        try {
            ensure(path);
            var json = Files.readString(path, StandardCharsets.UTF_8);
            if (json.isBlank()) return new ArrayList<>();
            List<T> list = GSON.fromJson(json, typeOfT);
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) { throw new RuntimeException(e); }
    }


    public static <T> void writeList(Path path, List<T> data) {
        try {
            ensure(path);
            var json = GSON.toJson(data);
            Files.writeString(path, json, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) { throw new RuntimeException(e); }
    }


    public static void ensure(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        if (!Files.exists(path)) Files.writeString(path, "", StandardCharsets.UTF_8);
    }
}