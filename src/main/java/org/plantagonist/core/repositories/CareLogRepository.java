package org.plantagonist.core.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import org.plantagonist.core.db.MongoConfig;
import org.plantagonist.core.models.CareLogEntry;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CareLogRepository extends BaseRepository<CareLogEntry> {

    public CareLogRepository() {
        super(MongoConfig.db().getCollection("care_logs", CareLogEntry.class));
        // helpful index: query by plant + sort by date
        coll.createIndex(Indexes.ascending("plantId", "dateIso"));
        coll.createIndex(Indexes.ascending("userId", "dateIso"));
    }

    public List<CareLogEntry> findByPlant(String plantId) {
        List<CareLogEntry> list = new ArrayList<>();
        coll.find(Filters.eq("plantId", plantId))
                .sort(Sorts.descending("dateIso"))
                .into(list);
        return list;
    }

    public List<CareLogEntry> findByUser(String userId) {
        List<CareLogEntry> list = new ArrayList<>();
        coll.find(Filters.eq("userId", userId))
                .sort(Sorts.descending("dateIso"))
                .into(list);
        return list;
    }

    public List<CareLogEntry> findByUserAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        List<CareLogEntry> list = new ArrayList<>();
        coll.find(Filters.and(
                Filters.eq("userId", userId),
                Filters.gte("dateIso", startDate.toString()),
                Filters.lte("dateIso", endDate.toString())
        )).sort(Sorts.descending("dateIso")).into(list);
        return list;
    }

    public List<CareLogEntry> findRecent(int limit) {
        List<CareLogEntry> list = new ArrayList<>();
        coll.find().sort(Sorts.descending("dateIso")).limit(limit).into(list);
        return list;
    }

    public List<CareLogEntry> findRecentByUser(String userId, int limit) {
        List<CareLogEntry> list = new ArrayList<>();
        coll.find(Filters.eq("userId", userId))
                .sort(Sorts.descending("dateIso"))
                .limit(limit)
                .into(list);
        return list;
    }
}