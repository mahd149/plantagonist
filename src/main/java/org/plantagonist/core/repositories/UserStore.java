package org.plantagonist.core.repositories;

import org.plantagonist.core.models.UserProfile;
import java.util.List;

public interface UserStore {
    UserProfile findByEmail(String email);
    UserProfile findByUsername(String username);
    UserProfile findById(String id);
    List<UserProfile> listAll();

    void insert(UserProfile u);
    void replaceById(String id, UserProfile u);
    long deleteById(String id);
}
