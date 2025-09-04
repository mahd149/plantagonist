package org.plantagonist.core.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.plantagonist.core.auth.CurrentUser;
import org.plantagonist.core.json.DateAdapters;
import org.plantagonist.core.models.UserProfile;
import org.plantagonist.core.storage.PathsConfig;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class UserProfileService {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.util.Date.class, DateAdapters.DESERIALIZER)
            .registerTypeAdapter(java.util.Date.class, DateAdapters.SERIALIZER)
            .setPrettyPrinting()
            .create();
    private static final Type LIST_TYPE = new TypeToken<List<UserProfile>>(){}.getType();

    private static Path getUserFile() {
        Path file = PathsConfig.userJson();
        try { Files.createDirectories(file.getParent()); } catch (IOException ignored) {}
        return file;
    }

    /* ---------- array IO ---------- */

    private static List<UserProfile> readAll() throws IOException {
        Path file = getUserFile();
        if (!Files.exists(file) || Files.size(file) == 0) return new ArrayList<>();
        try (Reader r = new FileReader(file.toFile())) {
            List<UserProfile> list = gson.fromJson(r, LIST_TYPE);
            if (list == null) return new ArrayList<>();
            // normalize & fill defaults
            for (UserProfile u : list) if (u != null) u.normalize();
            return list;
        }
    }

    private static void writeAll(List<UserProfile> list) throws IOException {
        Path file = getUserFile();
        try (Writer w = new FileWriter(file.toFile())) {
            gson.toJson(list, LIST_TYPE, w);
        }
    }

    /* ---------- public API ---------- */

    /** Load the profile for the current user; fallback to first entry; fallback to new default. */
    public static UserProfile loadUserProfile() {
        try {
            List<UserProfile> all = readAll();

            // 1) Try by CurrentUser id
            try {
                UserProfile cu = CurrentUser.get();
                if (cu != null && cu.getId() != null) {
                    for (UserProfile u : all) {
                        if (u != null && Objects.equals(u.getId(), cu.getId())) return u;
                    }
                }
                // 2) Try by email/username
                if (cu != null) {
                    for (UserProfile u : all) {
                        if (u == null) continue;
                        if (cu.getEmail() != null && cu.getEmail().equalsIgnoreCase(u.getEmail())) return u;
                        if (cu.getUsername() != null && cu.getUsername().equalsIgnoreCase(u.getUsername())) return u;
                    }
                }
            } catch (Throwable ignored) {}

            // 3) Fallback: first profile in the file
            if (!all.isEmpty() && all.get(0) != null) return all.get(0);

            // 4) Nothing found → default
            return new UserProfile();
        } catch (IOException e) {
            e.printStackTrace();
            return new UserProfile();
        }
    }

    /** Upsert the given profile back into user.json (array). */
    public static void saveUserProfile(UserProfile profile) {
        if (profile == null) return;
        profile.normalize();
        try {
            List<UserProfile> all = readAll();
            boolean replaced = false;

            // match by id if present
            if (profile.getId() != null) {
                for (int i = 0; i < all.size(); i++) {
                    UserProfile u = all.get(i);
                    if (u != null && Objects.equals(u.getId(), profile.getId())) {
                        profile.touchUpdated();
                        all.set(i, profile);
                        replaced = true;
                        break;
                    }
                }
            }
            // otherwise match by email/username
            if (!replaced) {
                for (int i = 0; i < all.size(); i++) {
                    UserProfile u = all.get(i);
                    if (u == null) continue;
                    boolean emailMatch = profile.getEmail() != null && profile.getEmail().equalsIgnoreCase(u.getEmail());
                    boolean userMatch  = profile.getUsername() != null && profile.getUsername().equalsIgnoreCase(u.getUsername());
                    if (emailMatch || userMatch) {
                        if (profile.getId() == null) profile.setId(u.getId());
                        profile.touchUpdated();
                        all.set(i, profile);
                        replaced = true;
                        break;
                    }
                }
            }
            // new entry
            if (!replaced) {
                profile.touchCreated();
                all.add(profile);
            }

            writeAll(all);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Extra utilities if you need them later */
    public static List<UserProfile> loadAllProfiles() {
        try { return readAll(); } catch (IOException e) { return List.of(); }
    }
    public static Optional<UserProfile> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return loadAllProfiles().stream().filter(u -> email.equalsIgnoreCase(u.getEmail())).findFirst();
    }
}
