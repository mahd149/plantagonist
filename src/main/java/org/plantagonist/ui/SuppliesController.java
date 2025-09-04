package org.plantagonist.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.converter.IntegerStringConverter;
import org.bson.types.ObjectId;
import org.plantagonist.core.auth.CurrentUser;
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

    private final ObservableList<SupplyItem> backing = FXCollections.observableArrayList();

    private final SupplyRepository supplyRepository = new SupplyRepository();
    private NotificationService notificationService;

    private String currentUserId() {
        return CurrentUser.get().getId(); // replace if your auth accessor differs
    }

    @FXML
    public void initialize() {
        try {
            notificationService = new NotificationService();
        } catch (Exception e) {
            System.out.println("Notification service not available: " + e.getMessage());
        }

        setupTable();
        reload();
    }

    private void setupTable() {
        // Status column (computed)
        statusColumn.setCellValueFactory(cellData -> {
            String status = determineStatus(cellData.getValue());
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
                        case "out":
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

        // Quantity column (inline editable with correct delta)
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        quantityColumn.setOnEditCommit(event -> {
            SupplyItem item = event.getRowValue();
            Integer oldVal = event.getOldValue();
            Integer newVal = event.getNewValue();
            if (oldVal == null) oldVal = 0;
            if (newVal == null) newVal = 0;

            int delta = newVal - oldVal; // compute BEFORE mutating the item

            try {
                ObjectId id = item.getId();
                supplyRepository.adjustQuantity(id, currentUserId(), delta);
                item.setQuantity(newVal);
                item.setLastRestocked(LocalDate.now()); // optional: treat edits as updates
                updateItemStatus(item);
                suppliesTable.refresh();
            } catch (Exception ex) {
                showAlert("Update failed", "Could not update quantity: " + ex.getMessage());
                suppliesTable.refresh();
            }
        });

        // Last restocked column (LocalDate -> string)
        lastRestockedColumn.setCellValueFactory(cd -> {
            LocalDate d = cd.getValue().getLastRestocked();
            return new javafx.beans.property.SimpleStringProperty(d == null ? "Never" : d.toString());
        });

        // Actions column
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
                setGraphic(empty ? null : pane);
            }
        });

        // Table
        suppliesTable.setEditable(true);
        suppliesTable.setItems(backing);
    }

    private String determineStatus(SupplyItem item) {
        if (item.getQuantity() <= 0) return "Out";
        if (item.getQuantity() <= item.getRefillBelow()) return "Low Supply";
        return "In Stock";
    }

    private void updateItemStatus(SupplyItem item) {
        String newStatus = determineStatus(item);
        item.setStatus(newStatus);

        // Notify if it transitions to Low Supply
        if ("Low Supply".equals(newStatus) && notificationService != null) {
            try {
                // Replace with your real notification hook if available
                System.out.println("ALERT: Low stock for " + item.getName() +
                        ". Current quantity: " + item.getQuantity());
            } catch (Exception e) {
                System.out.println("Could not send notification: " + e.getMessage());
            }
        }
    }

    // ===== UI actions =====
    @FXML
    private void addSupply() {
        try {
            String name = itemField.getText() == null ? "" : itemField.getText().trim();
            int quantity = Integer.parseInt(qtyField.getText());
            int refillBelow = Integer.parseInt(thresholdField.getText());

            if (name.isEmpty()) {
                showAlert("Error", "Please enter a supply item name.");
                return;
            }

            // Construct a new item (id assigned in model ctor; ensure userId set there)
            SupplyItem newItem = new SupplyItem(currentUserId(), name, quantity, refillBelow);
            supplyRepository.upsertByName(currentUserId(), newItem);

            reload();
            itemField.clear();
            qtyField.clear();
            thresholdField.clear();

        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter valid numbers for quantity and threshold.");
        } catch (Exception ex) {
            showAlert("Save failed", ex.getMessage());
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
                if (quantityToAdd == 0) return;

                supplyRepository.adjustQuantity(item.getId(), currentUserId(), quantityToAdd);

                item.setQuantity(item.getQuantity() + quantityToAdd);
                item.setLastRestocked(LocalDate.now()); // timestamp restock
                updateItemStatus(item);
                suppliesTable.refresh();
            } catch (NumberFormatException e) {
                showAlert("Error", "Please enter a valid number.");
            } catch (Exception ex) {
                showAlert("Restock failed", ex.getMessage());
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
            try {
                supplyRepository.delete(item.getId(), currentUserId());
                backing.remove(item);
            } catch (Exception ex) {
                showAlert("Delete failed", ex.getMessage());
            }
        }
    }

    @FXML
    private void reload() {
        try {
            backing.setAll(supplyRepository.findAll(currentUserId()));
            suppliesTable.refresh();
        } catch (Exception ex) {
            showAlert("Load failed", ex.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        // ensure dialog resizes for long text
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }
}
