package org.plantagonist.core.repositories;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.plantagonist.core.models.UserProfile;
import org.plantagonist.core.storage.PathsConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UserRepositoryJson implements UserStore {
    private final Path file;
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
//
            .create();
    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

    private static final Type LIST_TYPE = new TypeToken<List<UserProfile>>(){}.getType();

    public UserRepositoryJson() {
        this(null);
    }

    /** If file is null: ~/.plantagonist/users.json */
    public UserRepositoryJson(Path file) {
        this.file = (file != null) ? file : defaultPath();
        ensureFile();
    }

    private Path defaultPath() {
       // return Paths.get(System.getProperty("user.home"), ".plantagonist", "users.json");
        return PathsConfig.userJson();
    }

    private void ensureFile() {
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                saveAll(new ArrayList<>());
                securePermissionsIfPossible();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to init users.json at " + file, e);
        }
    }

    private void securePermissionsIfPossible() {
        try {
            // Best effort (POSIX only)
            Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(file, perms);
        } catch (Exception ignored) {}
    }

    // ---------- Core JSON IO ----------

    private List<UserProfile> loadAll() {
        rw.readLock().lock();
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<UserProfile> users = gson.fromJson(br, LIST_TYPE);
            if (users == null) users = new ArrayList<>();
            // normalize + backfill ids
            for (UserProfile u : users) {
                if (u.getId() == null || u.getId().isBlank()) u.setId(UUID.randomUUID().toString());
                u.normalize();
            }
            return users;
        } catch (IOException e) {
            throw new RuntimeException("Failed reading " + file, e);
        } finally {
            rw.readLock().unlock();
        }
    }

    private void saveAll(List<UserProfile> users) {
        rw.writeLock().lock();
        try {
            for (UserProfile u : users) {
                if (u.getId() == null || u.getId().isBlank()) u.setId(UUID.randomUUID().toString());
                u.normalize();
                if (u.getCreatedAt() == null) u.setCreatedAt(Date.from(Instant.now()));
                if (u.getUpdatedAt() == null) u.setUpdatedAt(Date.from(Instant.now()));
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (BufferedWriter bw = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                gson.toJson(users, LIST_TYPE, bw);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed writing " + file, e);
        } finally {
            rw.writeLock().unlock();
        }
    }

    // ---------- Lookups ----------

    @Override
    public UserProfile findByEmail(String email) {
        if (email == null) return null;
        String key = email.trim().toLowerCase();
        for (UserProfile u : loadAll()) {
            if (key.equals(u.getEmail())) return u;
        }
        return null;
    }

    @Override
    public UserProfile findByUsername(String username) {
        if (username == null) return null;
        String key = username.trim();
        for (UserProfile u : loadAll()) {
            if (key.equals(u.getUsername())) return u;
        }
        return null;
    }

    @Override
    public UserProfile findById(String id) {
        if (id == null) return null;
        for (UserProfile u : loadAll()) {
            if (id.equals(u.getId())) return u;
        }
        return null;
    }

    @Override
    public List<UserProfile> listAll() {
        return loadAll();
    }

    // ---------- Mutations ----------

    @Override
    public void insert(UserProfile u) {
        if (u == null) throw new IllegalArgumentException("user is null");
        List<UserProfile> users = loadAll();

        String email = (u.getEmail() == null) ? null : u.getEmail().trim().toLowerCase();
        String username = (u.getUsername() == null) ? null : u.getUsername().trim();

        if (email != null && users.stream().anyMatch(x -> email.equals(x.getEmail()))) {
            throw new IllegalStateException("Email already in use");
        }
        if (username != null && !username.isBlank()
                && users.stream().anyMatch(x -> username.equals(x.getUsername()))) {
            throw new IllegalStateException("Username already in use");
        }

        if (u.getId() == null || u.getId().isBlank()) u.setId(UUID.randomUUID().toString());
        u.normalize();
        u.touchCreated();

        users.add(u);
        saveAll(users);
    }

    @Override
    public void replaceById(String id, UserProfile u) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (u == null) throw new IllegalArgumentException("user is null");

        List<UserProfile> users = loadAll();
        int idx = -1;
        for (int i = 0; i < users.size(); i++) {
            if (id.equals(users.get(i).getId())) { idx = i; break; }
        }
        if (idx < 0) throw new NoSuchElementException("User not found: " + id);

        // enforce uniqueness if email/username changed
        String newEmail = (u.getEmail() == null) ? null : u.getEmail().trim().toLowerCase();
        String newUsername = (u.getUsername() == null) ? null : u.getUsername().trim();

        for (int i = 0; i < users.size(); i++) {
            if (i == idx) continue;
            if (newEmail != null && newEmail.equals(users.get(i).getEmail())) {
                throw new IllegalStateException("Email already in use");
            }
            if (newUsername != null && !newUsername.isBlank()
                    && newUsername.equals(users.get(i).getUsername())) {
                throw new IllegalStateException("Username already in use");
            }
        }

        u.setId(id);
        u.normalize();
        u.touchUpdated();
        users.set(idx, u);
        saveAll(users);
    }

    @Override
    public long deleteById(String id) {
        if (id == null) return 0L;
        List<UserProfile> users = loadAll();
        boolean removed = users.removeIf(x -> id.equals(x.getId()));
        if (removed) saveAll(users);
        return removed ? 1L : 0L;
    }
}
