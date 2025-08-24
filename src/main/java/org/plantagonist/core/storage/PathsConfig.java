package org.plantagonist.core.storage;


import java.nio.file.Path;


public class PathsConfig {
    public static Path appHome() {
        return Path.of(System.getProperty("user.home"), ".plantagonist");
    }
    public static Path dataDir() { return appHome().resolve("data"); }
    public static Path plantsJson() { return dataDir().resolve("plants.json"); }
    public static Path logsJson() { return dataDir().resolve("care_logs.json"); }
    public static Path suppliesJson() { return dataDir().resolve("supplies.json"); }
    public static Path userJson() { return dataDir().resolve("user.json"); }
}