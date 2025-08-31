package org.plantagonist.core.repositories;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.conversions.Bson;
import org.plantagonist.core.db.MongoConfig;
import org.plantagonist.core.models.CareTask;

import java.util.ArrayList;
import java.util.List;
import com.mongodb.client.model.Updates;


public class CareTaskRepository extends BaseRepository<CareTask> {
    public CareTaskRepository() {
        super(MongoConfig.db().getCollection("care_tasks", CareTask.class));
    }

    // convenience
    public void insertOne(CareTask t) { insert(t); }

    public List<CareTask> findByPlantIdAndType(String plantId, String type) {
        Bson filter = Filters.and(Filters.eq("plantId", plantId), Filters.eq("type", type));
        List<CareTask> out = new ArrayList<>();
        coll.find(filter).into(out);
        return out;
    }

    public void deleteByPlantIdAndType(String plantId, String type) {
        coll.deleteMany(Filters.and(Filters.eq("plantId", plantId), Filters.eq("type", type)));
    }

    public List<CareTask> findDueOrUpcoming() {
        List<CareTask> out = new ArrayList<>();
        coll.find(Filters.ne("status", "DONE")).into(out);
        return out;
    }

    /** wrappers so callers don't pass id getters everywhere */
    public CareTask findById(String id) { return super.findById(id, CareTask::getId); }
    public long deleteById(String id) { return super.deleteById(id, CareTask::getId); }
    public void replaceById(CareTask t) {
        if (t.getId() == null || t.getId().isBlank()) throw new IllegalArgumentException("Task id required");
        super.replace(t, t.getId(), CareTask::getId);
    }
    public void replaceById(String id, CareTask t) { super.replace(t, id, CareTask::getId); }

    /** Light‑weight status update (no full replace) */
    // CareTaskRepository.java
    public void updateStatus(String id, String status) {
        var result = coll.updateOne(Filters.eq("_id", id), Updates.set("status", status));
        System.out.println("[DB] updateStatus _id=" + id +
                " matched=" + result.getMatchedCount() +
                " modified=" + result.getModifiedCount());
    }



}
