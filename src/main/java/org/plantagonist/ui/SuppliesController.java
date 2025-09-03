package org.plantagonist.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.converter.IntegerStringConverter;
import org.plantagonist.core.models.SupplyItem;
import org.plantagonist.core.repositories.SupplyRepository;
import org.plantagonist.core.services.NotificationService;

import java.time.LocalDate;
import java.util.Optional;

public class SuppliesController {

    @FXML private TableView<SupplyItem> suppliesTable;
    @FXML private TableColumn<SupplyItem, String> statusColumn;
    @FXML private TableColumn<SupplyItem, String> nameColumn;
    @FXML private TableColumn<SupplyItem, Integer> quantityColumn;
    @FXML private TableColumn<SupplyItem, String> lastRestockedColumn;
    @FXML private TableColumn<SupplyItem, Void> actionsColumn;

    @FXML private TextField itemField;
    @FXML private TextField qtyField;
    @FXML private TextField thresholdField;

    private final SupplyRepository supplyRepository = new SupplyRepository();
    private NotificationService notificationService;

    @FXML
    public void initialize() {
        // Initialize notification service if available
        try {
            notificationService = new NotificationService();
        } catch (Exception e) {
            System.out.println("Notification service not available: " + e.getMessage());
        }

        setupTable();
        loadSupplies();
    }

    private void setupTable() {
        // Status column with custom cell factory for colored status badges
        statusColumn.setCellValueFactory(cellData -> {
            SupplyItem item = cellData.getValue();
            String status = determineStatus(item);
            return new javafx.beans.property.SimpleStringProperty(status);
        });

        statusColumn.setCellFactory(column -> new TableCell<SupplyItem, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status.toLowerCase()) {
                        case "low supply":
                            setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
                            break;
                        case "in stock":
                            setStyle("-fx-text-fill: #51cf66; -fx-font-weight: bold;");
                            break;
                        case "concrete":
                            setStyle("-fx-text-fill: #ffd43b; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: -color-subtext;");
                    }
                }
            }
        });

        // Name column
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Quantity column - editable
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        quantityColumn.setOnEditCommit(event -> {
            SupplyItem item = event.getRowValue();
            item.setQuantity(event.getNewValue());
            updateItemStatus(item);
            supplyRepository.adjustQuantity(item.getId(), event.getNewValue() - item.getQuantity());
        });

        // Last restocked column
        lastRestockedColumn.setCellValueFactory(cellData -> {
            SupplyItem item = cellData.getValue();
            String lastRestocked = item.getLastRestocked() != null ?
                    item.getLastRestocked().toString() : "Never";
            return new javafx.beans.property.SimpleStringProperty(lastRestocked);
        });

        // Actions column with buttons
        actionsColumn.setCellFactory(param -> new TableCell<SupplyItem, Void>() {
            private final Button restockButton = new Button("Restock");
            private final Button deleteButton = new Button("Delete");
            private final HBox pane = new HBox(restockButton, deleteButton);

            {
                pane.setSpacing(5);
                restockButton.getStyleClass().add("accent-btn");
                deleteButton.getStyleClass().addAll("nav-btn", "danger");
                restockButton.setStyle("-fx-font-size: 12px;");
                deleteButton.setStyle("-fx-font-size: 12px;");

                restockButton.setOnAction(event -> {
                    SupplyItem item = getTableView().getItems().get(getIndex());
                    handleRestock(item);
                });

                deleteButton.setOnAction(event -> {
                    SupplyItem item = getTableView().getItems().get(getIndex());
                    handleDelete(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });

        // Enable table editing
        suppliesTable.setEditable(true);
    }

    private String determineStatus(SupplyItem item) {
        if (item.getQuantity() <= 0) {
            return "Concrete";
        } else if (item.getQuantity() <= item.getRefillBelow()) {
            return "Low Supply";
        } else {
            return "In Stock";
        }
    }

    private void updateItemStatus(SupplyItem item) {
        String newStatus = determineStatus(item);

        // Send notification if status changed to Low Supply
        if ("Low Supply".equals(newStatus) && notificationService != null) {
            try {
                // Create a simple notification method since sendSupplyAlert doesn't exist
                System.out.println("ALERT: Low stock for " + item.getName() +
                        ". Current quantity: " + item.getQuantity());
            } catch (Exception e) {
                System.out.println("Could not send notification: " + e.getMessage());
            }
        }
    }

    @FXML
    private void addSupply() {
        try {
            String name = itemField.getText().trim();
            int quantity = Integer.parseInt(qtyField.getText());
            int refillBelow = Integer.parseInt(thresholdField.getText());

            if (name.isEmpty()) {
                showAlert("Error", "Please enter a supply item name.");
                return;
            }

            SupplyItem newItem = new SupplyItem(java.util.UUID.randomUUID().toString(),
                    name, quantity, refillBelow);
            // Use upsertByName instead of save
            supplyRepository.upsertByName(newItem);

            // Reload all supplies to refresh the table
            loadSupplies();

            // Clear input fields
            itemField.clear();
            qtyField.clear();
            thresholdField.clear();

        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter valid numbers for quantity and threshold.");
        }
    }

    private void handleRestock(SupplyItem item) {
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Restock Item");
        dialog.setHeaderText("Restock " + item.getName());
        dialog.setContentText("Enter quantity to add:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(quantityStr -> {
            try {
                int quantityToAdd = Integer.parseInt(quantityStr);
                // Use adjustQuantity method from repository
                supplyRepository.adjustQuantity(item.getId(), quantityToAdd);

                // Update the item locally for display
                item.setQuantity(item.getQuantity() + quantityToAdd);
                item.setLastRestocked(LocalDate.now());

                suppliesTable.refresh();
            } catch (NumberFormatException e) {
                showAlert("Error", "Please enter a valid number.");
            }
        });
    }

    private void handleDelete(SupplyItem item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Supply Item");
        alert.setHeaderText("Delete " + item.getName());
        alert.setContentText("Are you sure you want to delete this item?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Use the base repository delete method
            supplyRepository.delete(item.getId());
            suppliesTable.getItems().remove(item);
        }
    }

    private void loadSupplies() {
        suppliesTable.getItems().clear();
        suppliesTable.getItems().addAll(supplyRepository.findAll());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}