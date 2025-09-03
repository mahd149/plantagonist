package org.plantagonist.core.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.plantagonist.core.db.MongoConfig;
import org.plantagonist.core.models.JournalEntry;

import java.util.ArrayList;
import java.util.List;

public class JournalRepository extends BaseRepository<JournalEntry> {

    private static final String COLLECTION_NAME = "journal_entries";

    public JournalRepository() {
        super(getCollection());
    }

    private static MongoCollection<JournalEntry> getCollection() {
        return MongoConfig.db().getCollection(COLLECTION_NAME, JournalEntry.class);
    }

    // Override the idFilter to use JournalEntry's getId method
    @Override
    protected Bson idFilter(String id) {
        return Filters.eq("_id", id);
    }

    public List<JournalEntry> getAll() {
        return findAll();
    }

    public void insertOne(JournalEntry entry) {
        insert(entry);
    }

    public List<JournalEntry> findByUserId(String userId) {
        return findByField("userId", userId);
    }

    public List<JournalEntry> findByPlantId(String plantId) {
        return findByField("plantId", plantId);
    }

    public List<JournalEntry> findByUserIdAndPlantId(String userId, String plantId) {
        Bson filter = Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("plantId", plantId)
        );
        List<JournalEntry> out = new ArrayList<>();
        coll.find(filter).into(out);
        return out;
    }

    public List<JournalEntry> findRecentByUserId(String userId, int limit) {
        Bson filter = Filters.eq("userId", userId);
        List<JournalEntry> out = new ArrayList<>();
        coll.find(filter)
                .sort(Filters.eq("entryDate", -1)) // Sort by entryDate descending
                .limit(limit)
                .into(out);
        return out;
    }

    public void save(JournalEntry entry) {
        // For update/replace functionality
        if (entry.getId() != null) {
            replace(entry, entry.getId(), JournalEntry::getId);
        } else {
            insert(entry);
        }
    }
}