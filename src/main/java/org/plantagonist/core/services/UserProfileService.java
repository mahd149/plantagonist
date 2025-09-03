package org.plantagonist.core.services;

import com.google.gson.Gson;
import org.plantagonist.core.models.UserProfile;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class UserProfileService {

    private static final String PROFILE_FILE = "userProfile.json";

    public static UserProfile loadUserProfile() {
        try (FileReader reader = new FileReader(PROFILE_FILE)) {
            return new Gson().fromJson(reader, UserProfile.class);
        } catch (IOException e) {
            // Return default profile if file missing
            return new UserProfile();
        }
    }

    public static void saveUserProfile(UserProfile profile) {
        try (FileWriter writer = new FileWriter(PROFILE_FILE)) {
            new Gson().toJson(profile, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
