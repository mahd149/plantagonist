// org/plantagonist/core/repositories/SupplyRepository.java
package org.plantagonist.core.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.UpdateOptions;
import org.plantagonist.core.db.MongoConfig;
import org.plantagonist.core.models.SupplyItem;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

public class SupplyRepository extends BaseRepository<SupplyItem> {

    public SupplyRepository() {
        super(MongoConfig.db().getCollection("supplies", SupplyItem.class));
        coll.createIndex(Indexes.ascending("name"));
    }

    public List<SupplyItem> findLowStock() {
        List<SupplyItem> list = new ArrayList<>();
        coll.find(Filters.expr(new org.bson.Document("$lte",
                List.of("$quantity", "$refillBelow")))).into(list);
        return list;
    }

    /** Increase/decrease quantity (delta can be negative). */
    public void adjustQuantity(String id, int delta) {
        Bson filter = Filters.eq("id", id);
        coll.updateOne(filter, Updates.inc("quantity", delta));
    }

    /** Upsert by name (optional helper). */
    public void upsertByName(SupplyItem item) {
        coll.updateOne(Filters.eq("name", item.getName()),
                Updates.combine(
                        Updates.setOnInsert("id", item.getId()),
                        Updates.set("quantity", item.getQuantity()),
                        Updates.set("refillBelow", item.getRefillBelow())
                ),
                new UpdateOptions().upsert(true));
    }
}
