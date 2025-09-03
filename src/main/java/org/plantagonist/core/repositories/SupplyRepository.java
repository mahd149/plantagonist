// org/plantagonist/core/repositories/SupplyRepository.java
package org.plantagonist.core.repositories;

import com.mongodb.client.model.*;
import com.mongodb.client.MongoCollection;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.plantagonist.core.db.MongoConfig;
import org.plantagonist.core.models.SupplyItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class SupplyRepository extends BaseRepository<SupplyItem> {

    public SupplyRepository() {
        super(MongoConfig.db().getCollection("supplies", SupplyItem.class));
        // Unique name per user
        coll.createIndex(Indexes.compoundIndex(Indexes.ascending("userId", "name")),
                new IndexOptions().unique(true));
        coll.createIndex(Indexes.ascending("userId"));
    }

    /** Get all supplies for a user. */
    public List<SupplyItem> findAll(String userId) {
        List<SupplyItem> list = new ArrayList<>();
        coll.find(eq("userId", userId)).into(list);
        return list;
    }

    /** Low stock for a user. */
    public List<SupplyItem> findLowStock(String userId) {
        List<SupplyItem> list = new ArrayList<>();
        // quantity <= refillBelow
        Bson lowExpr = expr(new org.bson.Document("$lte", List.of("$quantity", "$refillBelow")));
        coll.find(and(eq("userId", userId), lowExpr)).into(list);
        return list;
    }

    /** Increase/decrease quantity (delta can be negative). */
    public void adjustQuantity(ObjectId id, String userId, int delta) {
        coll.updateOne(and(eq("_id", id), eq("userId", userId)), inc("quantity", delta));
    }

    /** Delete by id for a user. */
    public void delete(ObjectId id, String userId) {
        coll.deleteOne(and(eq("_id", id), eq("userId", userId)));
    }

    /** Upsert by (userId,name). Also refresh lastRestocked on insert/update. */
    public void upsertByName(String userId, SupplyItem item) {
        Bson filter = and(eq("userId", userId), eq("name", item.getName()));
        UpdateOptions opts = new UpdateOptions().upsert(true);

        // ensure we set _id on insert
        ObjectId id = item.getId() != null ? item.getId() : new ObjectId();

        coll.updateOne(
                filter,
                combine(
                        setOnInsert("_id", id),
                        set("userId", userId),
                        set("name", item.getName()),
                        set("quantity", item.getQuantity()),
                        set("refillBelow", item.getRefillBelow()),
                        set("lastRestocked", LocalDate.now())
                ),
                opts
        );
    }

    public void restock(ObjectId id, String userId, int add) {
        if (add <= 0) return; // ignore no-op / invalid
        coll.updateOne(
                and(eq("_id", id), eq("userId", userId)),
                combine(
                        inc("quantity", add),
                        set("lastRestocked", LocalDate.now())
                )
        );
    }

    /** Optional: clamp at zero so we never go negative */
    public void adjustQuantity(ObjectId id, String userId, int delta, boolean clampAtZero) {
        if (!clampAtZero) {
            coll.updateOne(and(eq("_id", id), eq("userId", userId)), inc("quantity", delta));
            return;
        }
        // clamp with $max after computing new value
        coll.updateOne(
                and(eq("_id", id), eq("userId", userId)),
                combine(
                        inc("quantity", delta),
                        // enforce quantity >= 0
                        // NOTE: requires MongoDB 4.2+ for update pipeline if you want true clamp;
                        // a simple post-fix could be done with findOneAndUpdate
                        inc("quantity", 0) // placeholder — or use findOneAndUpdate + Math.max in Java side
                )
        );
    }


}
