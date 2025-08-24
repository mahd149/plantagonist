package org.plantagonist.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.plantagonist.core.services.SuggestionService;
import org.plantagonist.core.services.WeatherService;

public class DashboardController {

    @FXML private Label weatherAdvice;

    @FXML
    public void initialize() {
        // ... load plants/tasks here if you have that already ...

        // Auto-detect location via WeatherAPI (q=auto:ip) and suggest
        try {
            WeatherService ws = new WeatherService();
            WeatherService.WeatherNow w = ws.getNowAuto();   // <— auto-detect by IP
            String advice = new SuggestionService().waterAdvice(w.precipMm, w.tempC);
            weatherAdvice.setText(String.format("%s (%.1f°C • %s)",
                    advice, w.tempC, (w.locationName != null ? w.locationName : "your area")));
        } catch (Exception e) {
            weatherAdvice.setText("Weather unavailable: " + e.getMessage());
        }
    }
}
