package org.plantagonist.core.repositories;

import com.mongodb.client.model.Filters;
import org.plantagonist.core.db.MongoConfig;
import org.plantagonist.core.models.UserProfile;

public class UserRepository extends BaseRepository<UserProfile> {
    public UserRepository() {
        super(MongoConfig.db().getCollection("users", UserProfile.class));
    }

    public UserProfile findByEmail(String email) {
        if (email == null) return null;
        return coll.find(Filters.eq("email", email.trim().toLowerCase())).first();
    }

    public UserProfile findByUsername(String username) {
        if (username == null) return null;
        return coll.find(Filters.eq("username", username.trim())).first();
    }

    public void insertOne(UserProfile u) { insert(u); }
}
