package org.plantagonist.core.repositories;


import com.mongodb.client.MongoCollection;
import org.plantagonist.core.db.MongoConfig;
import org.plantagonist.core.models.Plant;


public class PlantRepository extends BaseRepository<Plant> {
    public PlantRepository() {
        super(MongoConfig.db().getCollection("plants", Plant.class));
    }
}