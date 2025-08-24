module org.plantagonist {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // HTTP client for APIs
    requires java.net.http;

    // JSON
    requires com.google.gson;

    // ControlsFX (if used in UI)
    requires org.controlsfx.controls;

    // MongoDB Java Driver 5.x
    // (sync client + bson; include core to avoid linkage issues)
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.bson;
    requires org.mongodb.driver.core;

    // --- Reflection openings ---
    // FXML controllers are constructed reflectively
    opens org.plantagonist.ui to javafx.fxml;

    // Models are deserialized by Gson & Mongo (both use reflection)
    opens org.plantagonist.core.models to com.google.gson, org.mongodb.bson;

    // If you put DTOs/configs elsewhere, open those packages similarly.
    // Example: opens org.plantagonist.core.services to com.google.gson;

    // --- Exports (what other modules may import) ---
    exports org.plantagonist;        // App launcher package
    exports org.plantagonist.ui;     // if other modules need UI APIs (optional)
    exports org.plantagonist.core.models; // optional; export if used externally
}
