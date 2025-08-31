package org.plantagonist.core.repositories;

import org.plantagonist.core.db.MongoConfig;
import org.plantagonist.core.models.Plant;

import java.util.List;

public class PlantRepository extends BaseRepository<Plant> {
    public PlantRepository() {
        super(MongoConfig.db().getCollection("plants", Plant.class));
    }

    // Convenience wrappers so callers don’t need idGetter lambdas
    public void insertOne(Plant p) { insert(p); }

    @Override
    public List<Plant> findAll() { return super.findAll(); }

    public Plant findById(String id) { return super.findById(id, Plant::getId); }

    public long deleteById(String id) { return super.deleteById(id, Plant::getId); }

    /** Replace using the id inside the plant */
//    public void replaceById(Plant p) {
//        if (p.getId() == null || p.getId().isBlank()) {
//            throw new IllegalArgumentException("Plant id is required for replace");
//        }
//        super.replace(p, p.getId(), Plant::getId);
//    }

    /** Replace using an explicit id (useful when editing a copy) */
    public void replaceById(String id, Plant p) {
        super.replace(p, id, Plant::getId);
    }
}
