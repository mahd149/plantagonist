package org.plantagonist.core.db;


import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;


public class MongoConfig {
    private static MongoClient client;
    private static MongoDatabase db;


    public static synchronized MongoDatabase db() {
        if (db == null) {
            String uri = System.getenv().getOrDefault("MONGODB_URI", "mongodb://localhost:27017");
            String dbName = System.getenv().getOrDefault("MONGODB_DB", "plantagonist");
            CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .codecRegistry(pojoCodecRegistry)
                    .build();
            client = MongoClients.create(settings);
            db = client.getDatabase(dbName);
        }
        return db;
    }
}