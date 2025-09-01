package org.plantagonist.core.repositories;

import org.plantagonist.core.models.UserProfile;

import java.util.List;

public class UserRepositoryMongoAdapter implements UserStore {
    private final UserRepository mongo;

    public UserRepositoryMongoAdapter(UserRepository mongo) {
        this.mongo = mongo;
    }

    @Override public UserProfile findByEmail(String email) { return mongo.findByEmail(email); }
    @Override public UserProfile findByUsername(String username) { return mongo.findByUsername(username); }
    @Override public UserProfile findById(String id) { return mongo.findById(id, UserProfile::getId); }
    @Override public List<UserProfile> listAll() { return mongo.findAll(); }

    @Override public void insert(UserProfile u) { mongo.insert(u); }
    @Override public void replaceById(String id, UserProfile u) { mongo.replace(u, id, UserProfile::getId); }
    @Override public long deleteById(String id) { return mongo.deleteById(id, UserProfile::getId); }
}
