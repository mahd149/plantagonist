// org/plantagonist/core/models/SupplyItem.java
package org.plantagonist.core.models;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;
import org.bson.types.ObjectId;
import org.bson.BsonType;

import java.time.LocalDate;

public class SupplyItem {
    @BsonId
    private ObjectId id;

    private String userId;          // NEW: owner
    private String name;
    private int quantity;
    private int refillBelow;
    private String status;

    // Store as string to avoid time codec plumbing
//    @BsonRepresentation(BsonType.STRING)
    private LocalDate lastRestocked;

    public SupplyItem() { }

    public SupplyItem(String userId, String name, int quantity, int refillBelow) {
        this.id = new ObjectId();
        this.userId = userId;
        this.name = name;
        this.quantity = quantity;
        this.refillBelow = refillBelow;
        this.lastRestocked = LocalDate.now();
    }

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public String getIdHex() { return id != null ? id.toHexString() : null; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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
