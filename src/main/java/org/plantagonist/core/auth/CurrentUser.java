package org.plantagonist.core.auth;

import org.plantagonist.core.models.UserProfile;

public class CurrentUser {
    private static UserProfile currentUser;

    public static UserProfile get() {
        return currentUser;
    }

    public static void set(UserProfile user) {
        currentUser = user;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void clear() {
        currentUser = null;
    }
}