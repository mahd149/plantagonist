package org.plantagonist.core.repositories;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.conversions.Bson;
import org.plantagonist.core.db.MongoConfig;
import org.plantagonist.core.models.CareTask;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    public List<CareTask> findByUserId(String userId) {
        return findByUserId(userId, CareTask::getUserId);
    }

    public List<CareTask> findByUserIdAndStatus(String userId, String status) {
        Bson filter = Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("status", status)
        );
        List<CareTask> out = new ArrayList<>();
        coll.find(filter).into(out);
        return out;
    }

    public List<CareTask> findByUserIdAndDate(String userId, LocalDate date) {
        Bson filter = Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("dueDate", date)
        );
        List<CareTask> out = new ArrayList<>();
        coll.find(filter).into(out);
        return out;
    }

    public List<CareTask> findByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        Bson filter = Filters.and(
                Filters.eq("userId", userId),
                Filters.gte("dueDate", startDate),
                Filters.lte("dueDate", endDate)
        );
        List<CareTask> out = new ArrayList<>();
        coll.find(filter).sort(com.mongodb.client.model.Sorts.ascending("dueDate")).into(out);
        return out;
    }

    public List<CareTask> findDueOrUpcoming(String userId) {
        Bson filter = Filters.and(
                Filters.eq("userId", userId),
                Filters.ne("status", "DONE"),
                Filters.ne("status", "CANCELLED")
        );
        List<CareTask> out = new ArrayList<>();
        coll.find(filter).sort(com.mongodb.client.model.Sorts.ascending("dueDate")).into(out);
        return out;
    }

    public void deleteByPlantIdAndType(String plantId, String type, String userId) {
        Bson filter = Filters.and(
                Filters.eq("plantId", plantId),
                Filters.eq("type", type),
                Filters.eq("userId", userId)
        );
        coll.deleteMany(filter);
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
    public void updateStatus(String id, String status) {
        var result = coll.updateOne(Filters.eq("_id", id), Updates.set("status", status));
        System.out.println("[DB] updateStatus _id=" + id +
                " matched=" + result.getMatchedCount() +
                " modified=" + result.getModifiedCount());
    }

    public void updateStatusAndLastCompleted(String id, String status, LocalDate lastCompleted) {
        var result = coll.updateOne(Filters.eq("_id", id),
                Updates.combine(
                        Updates.set("status", status),
                        Updates.set("lastCompleted", lastCompleted)
                ));
        System.out.println("[DB] updateStatusAndLastCompleted _id=" + id +
                " matched=" + result.getMatchedCount() +
                " modified=" + result.getModifiedCount());
    }
}