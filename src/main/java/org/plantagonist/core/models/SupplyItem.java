package org.plantagonist.core.models;

import java.time.LocalDate;

public class SupplyItem {
    private String id;
    private String name;
    private int quantity;
    private int refillBelow;
    private String status;
    private LocalDate lastRestocked;

    public SupplyItem() {}

    public SupplyItem(String id, String name, int quantity, int refillBelow) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.refillBelow = refillBelow;
        this.lastRestocked = LocalDate.now();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getRefillBelow() { return refillBelow; }
    public void setRefillBelow(int refillBelow) { this.refillBelow = refillBelow; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getLastRestocked() { return lastRestocked; }
    public void setLastRestocked(LocalDate lastRestocked) { this.lastRestocked = lastRestocked; }
}